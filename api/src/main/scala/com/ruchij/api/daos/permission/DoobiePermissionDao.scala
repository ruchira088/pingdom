package com.ruchij.api.daos.permission

import com.ruchij.core.daos.doobie.DoobieCustomMappings._
import com.ruchij.api.daos.permission.models.Permission
import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator

object DoobiePermissionDao extends PermissionDao[ConnectionIO] {

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
    sql"""
      SELECT created_at, modified_at, user_id, account_id, permission_type, granted_by FROM permission
        WHERE user_id = $userId
    """
      .query[Permission]
      .to[List]

}