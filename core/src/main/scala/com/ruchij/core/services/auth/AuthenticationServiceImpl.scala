package com.ruchij.core.services.auth

import cats.effect.Sync
import cats.implicits._
import cats.{Applicative, ApplicativeError, ~>}
import com.ruchij.core.config.AuthenticationConfiguration
import com.ruchij.core.daos.auth.AuthenticationTokenDao
import com.ruchij.core.daos.auth.models.AuthenticationToken
import com.ruchij.core.daos.credentials.CredentialsDao
import com.ruchij.core.daos.user.UserDao
import com.ruchij.core.daos.user.models.{Email, User}
import com.ruchij.core.exceptions.{AuthenticationException, ResourceNotFoundException}
import com.ruchij.core.services.hash.PasswordHashingService
import com.ruchij.core.syntax._
import com.ruchij.core.types.{JodaClock, RandomGenerator}

class AuthenticationServiceImpl[F[_]: Sync: JodaClock, T[_]](
  passwordHashingService: PasswordHashingService[F],
  userDao: UserDao[T],
  credentialsDao: CredentialsDao[T],
  authenticationTokenDao: AuthenticationTokenDao[F],
  authenticationConfiguration: AuthenticationConfiguration
)(implicit transaction: T ~> F)
    extends AuthenticationService[F] {

  override def login(email: Email, password: String): F[AuthenticationToken] =
    for {
      user <- transaction(userDao.findByEmail(email))
        .toF[Throwable](ResourceNotFoundException(s"User not found with email: ${email.value}"))

      credentials <- transaction(credentialsDao.findByUserId(user.id))
        .toF[Throwable](
          new IllegalStateException(s"Unable to find credentials for user. id=${user.id}, email=${email.value}")
        )

      isPasswordMatch <- passwordHashingService.checkPassword(password, credentials.saltedPasswordHash)
      _ <- if (isPasswordMatch) Applicative[F].unit
      else ApplicativeError[F, Throwable].raiseError(AuthenticationException("Invalid password"))

      timestamp <- JodaClock[F].currentTimestamp
      secret <- RandomGenerator.uuidGenerator[F].generate.map(_.toString)

      authenticationToken = AuthenticationToken(
        timestamp,
        timestamp.plus(authenticationConfiguration.sessionDuration.toMillis),
        user.id,
        secret,
        0
      )

      _ <- authenticationTokenDao.save(authenticationToken)

    } yield authenticationToken

  override def authenticate(userId: String, secret: String): F[User] =
    authenticationTokenDao
      .findByUserIdAndSecret(userId, secret)
      .toF[Throwable](AuthenticationException("Authentication token not found"))
      .flatMap {
        case AuthenticationToken(issuedAt, expiresAt, userId, secret, renewals) =>
          JodaClock[F].currentTimestamp
            .flatMap { timestamp =>
              if (timestamp.isBefore(expiresAt))
                authenticationTokenDao.save {
                  AuthenticationToken(
                    issuedAt,
                    timestamp.plus(authenticationConfiguration.sessionDuration.toMillis),
                    userId,
                    secret,
                    renewals + 1
                  )
                } else
                logout(userId, secret)
                  .productR[Int] {
                    ApplicativeError[F, Throwable].raiseError(
                      AuthenticationException("Authentication token is expired")
                    )
                  }
            }
            .productR {
              transaction(userDao.findById(userId))
                .toF[Throwable](ResourceNotFoundException(s"User not found with id = $userId"))
            }
      }

  override def logout(userId: String, secret: String): F[AuthenticationToken] =
    authenticationTokenDao
      .removeByUserIdAndSecret(userId, secret)
      .toF[Throwable](AuthenticationException("Authentication token not found"))
}
