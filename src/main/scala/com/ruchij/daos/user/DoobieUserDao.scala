package com.ruchij.daos.user

import com.ruchij.daos.user.models.{Email, User}
import com.ruchij.daos.doobie.DoobieCustomMappings._
import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator

object DoobieUserDao extends UserDao[ConnectionIO] {
  override def save(user: User): ConnectionIO[Int] =
    sql"""
      INSERT INTO user_details (id, created_at, modified_at, account_id, first_name, last_name, email)
        VALUES (
          ${user.id},
          ${user.createdAt},
          ${user.modifiedAt},
          ${user.accountId},
          ${user.firstName},
          ${user.lastName},
          ${user.email}
        )
    """
      .update
      .run

  override def findByEmail(email: Email): ConnectionIO[Option[User]] =
    sql"""
      SELECT id, created_at, modified_at, account_id, first_name, last_name, email
        FROM user_details
        WHERE email = $email
    """
      .query[User]
      .option
}
