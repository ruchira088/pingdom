package com.ruchij.core.daos.permission

import com.ruchij.core.daos.permission.models.Permission

trait PermissionDao[F[_]] {

  def save(permission: Permission): F[Int]

  def findByUserId(userId: String): F[List[Permission]]

}
