package com.ruchij.daos.permission

import com.ruchij.daos.permission.models.Permission

trait PermissionDao[F[_]] {
  def save(permission: Permission): F[Int]

  def findByUserId(userId: String): F[List[Permission]]
}
