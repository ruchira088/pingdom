package com.ruchij.core.services.authorization

import com.ruchij.core.daos.permission.models.{Permission, PermissionType}

trait AuthorizationService[F[_]] {

  def grantPermission(
    userId: String,
    accountId: String,
    permissionType: PermissionType,
    grantedBy: Option[String]
  ): F[Permission]

  def withPermission[A](userId: String, accountId: String, permissionType: PermissionType)(block: => F[A]): F[A]

}
