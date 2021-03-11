package com.ruchij.api.services.user

import cats.effect.Sync
import cats.implicits._
import cats.{Monad, ~>}
import com.ruchij.api.daos.credentials.CredentialsDao
import com.ruchij.api.daos.credentials.models.Credentials
import com.ruchij.api.services.user.models.Password
import com.ruchij.core.daos.account.AccountDao
import com.ruchij.core.daos.account.models.Account
import com.ruchij.core.daos.permission.models.PermissionType
import com.ruchij.core.daos.user.UserDao
import com.ruchij.core.daos.user.models.{Email, User}
import com.ruchij.core.exceptions.ResourceConflictException
import com.ruchij.core.services.authorization.AuthorizationService
import com.ruchij.core.services.hash.PasswordHashingService
import com.ruchij.core.syntax._
import com.ruchij.core.types.{JodaClock, RandomGenerator}

import java.util.UUID

class UserServiceImpl[F[_]: Sync: JodaClock, T[_]: Monad](
  passwordHashingService: PasswordHashingService[F],
  authorizationService: AuthorizationService[F],
  userDao: UserDao[T],
  accountDao: AccountDao[T],
  credentialsDao: CredentialsDao[T]
)(implicit transaction: T ~> F)
    extends UserService[F] {

  override def createNewUser(firstName: String, lastName: String, email: Email, password: Password): F[User] =
    for {
      mayBeExistingUser <- transaction { userDao.findByEmail(email) }
      _ <- mayBeExistingUser.toEmptyF[Throwable, F] { _ =>
        ResourceConflictException(s"User already exists with email: ${email.value}")
      }

      accountId <- RandomGenerator[F, UUID].generate.map(_.toString)
      userId <- RandomGenerator[F, UUID].generate.map(_.toString)

      timestamp <- JodaClock[F].currentTimestamp

      account = Account(accountId, timestamp, timestamp, s"$firstName $lastName")
      user = User(userId, timestamp, timestamp, accountId, firstName, lastName, email)

      saltedPasswordHash <- passwordHashingService.hash(password.value)
      credentials = Credentials(userId, timestamp, timestamp, saltedPasswordHash)

      _ <- transaction {
        accountDao
          .save(account)
          .product(userDao.save(user))
          .product(credentialsDao.save(credentials))
      }

      _ <- authorizationService.grantPermission(userId, accountId, PermissionType.Administrator, None)
    } yield user

}
