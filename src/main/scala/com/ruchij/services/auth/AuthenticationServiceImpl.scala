package com.ruchij.services.auth

import cats.effect.Sync
import cats.implicits._
import cats.{Applicative, ApplicativeError, ~>}
import com.ruchij.daos.auth.AuthenticationTokenDao
import com.ruchij.daos.auth.models.AuthenticationToken
import com.ruchij.daos.credentials.CredentialsDao
import com.ruchij.daos.user.UserDao
import com.ruchij.daos.user.models.{Email, User}
import com.ruchij.exceptions.{AuthenticationException, ResourceNotFoundException}
import com.ruchij.services.hash.PasswordHashingService
import com.ruchij.syntax._
import com.ruchij.types.{JodaClock, RandomGenerator}

class AuthenticationServiceImpl[F[_]: Sync: JodaClock, T[_]](
  passwordHashingService: PasswordHashingService[F],
  userDao: UserDao[T],
  credentialsDao: CredentialsDao[T],
  authenticationTokenDao: AuthenticationTokenDao[T]
)(implicit transaction: T ~> F)
    extends AuthenticationService[F] {

  override def login(email: Email, password: String): F[AuthenticationToken] =
    for {
      user <-
        transaction(userDao.findByEmail(email))
          .toF[Throwable](ResourceNotFoundException(s"User not found with email: ${email.value}"))

      credentials <-
        transaction(credentialsDao.findByUserId(user.id))
          .toF[Throwable](new IllegalStateException(s"Unable to find credentials for user. id=${user.id}, email=${email.value}"))

      isPasswordMatch <- passwordHashingService.checkPassword(password, credentials.saltedPasswordHash)
      _ <-
        if (isPasswordMatch) Applicative[F].unit
        else ApplicativeError[F, Throwable].raiseError(AuthenticationException("Invalid password"))

      timestamp <- JodaClock[F].currentTimestamp
      secret <- RandomGenerator.uuidGenerator[F].generate.map(_.toString)

      authenticationToken = AuthenticationToken(timestamp, ???, user.id, secret, 0)
    }
    yield authenticationToken

  override def authenticate(userId: String, secret: String): F[User] = ???

}
