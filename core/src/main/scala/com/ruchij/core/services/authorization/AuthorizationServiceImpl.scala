package com.ruchij.core.services.authorization

import cats.implicits._
import cats.{MonadError, ~>}
import com.ruchij.core.daos.permission.PermissionDao
import com.ruchij.core.daos.permission.models.{Permission, PermissionType}
import com.ruchij.core.exceptions.AuthorizationException
import com.ruchij.core.types.JodaClock

class AuthorizationServiceImpl[F[_]: JodaClock: MonadError[*[_], Throwable], T[_]](permissionDao: PermissionDao[T])(
  implicit transaction: T ~> F
) extends AuthorizationService[F] {

  override def grantPermission(
    userId: String,
    accountId: String,
    permissionType: PermissionType,
    grantedBy: Option[String]
  ): F[Permission] =
    for {
      timestamp <- JodaClock[F].currentTimestamp

      permission = Permission(timestamp, timestamp, userId, accountId, permissionType, grantedBy)

      _ <- transaction(permissionDao.save(permission))
    } yield permission

  override def withPermission[A](userId: String, accountId: String, permissionType: PermissionType)(
    block: => F[A]
  ): F[A] =
    transaction(permissionDao.find(userId, accountId, permissionType))
      .flatMap {
        case None =>
          MonadError[F, Throwable].raiseError {
            AuthorizationException(
              s"user ID = $userId does NOT have $permissionType permission to account ID = $accountId"
            )
          }

        case _ => block
      }
}
