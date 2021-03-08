package com.ruchij.core.daos.permission

import cats.implicits._
import com.ruchij.core.daos.doobie.DoobieCustomMappings._
import com.ruchij.core.daos.permission.models.{Permission, PermissionType}
import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator
import doobie.util.fragments.{in, whereAndOpt}

object DoobiePermissionDao extends PermissionDao[ConnectionIO] {

  val SelectQuery = fr"SELECT created_at, modified_at, user_id, account_id, permission_type, granted_by FROM permission"

  override def save(permission: Permission): ConnectionIO[Int] =
    sql"""
      INSERT INTO permission (created_at, modified_at, user_id, account_id, permission_type, granted_by)
        VALUES (
          ${permission.createdAt},
          ${permission.modifiedAt},
          ${permission.userId},
          ${permission.accountId},
          ${permission.permissionType},
          ${permission.grantedBy}
        )
    """.update.run

  override def findByUserId(userId: String): ConnectionIO[List[Permission]] =
    (SelectQuery ++ fr"WHERE user_id = $userId")
      .query[Permission]
      .to[List]

  override def find(
    userId: String,
    accountId: String,
    permissionType: PermissionType
  ): ConnectionIO[Option[Permission]] =
    (SelectQuery ++
      whereAndOpt(
        Some(fr"WHERE user_id = $userId AND account_id = $accountId"),
        PermissionType.ordering
          .dropWhile(_ != permissionType)
          .toNel
          .map(permissionTypes => in(fr"permission_type", permissionTypes))
      ) ++ fr"LIMIT 1")
      .query[Permission]
      .option

}
