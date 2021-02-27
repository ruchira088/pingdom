package com.ruchij.core.daos.user

import com.ruchij.core.daos.user.models.{Email, User}
import com.ruchij.core.daos.doobie.DoobieCustomMappings._
import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator
import doobie.util.fragment

object DoobieUserDao extends UserDao[ConnectionIO] {

  val SelectQuery: fragment.Fragment =
    fr"""
        SELECT id, created_at, modified_at, account_id, first_name, last_name, email
        FROM user_info
      """

  override def save(user: User): ConnectionIO[Int] =
    sql"""
      INSERT INTO user_info (id, created_at, modified_at, account_id, first_name, last_name, email)
        VALUES (
          ${user.id},
          ${user.createdAt},
          ${user.modifiedAt},
          ${user.accountId},
          ${user.firstName},
          ${user.lastName},
          ${user.email}
        )
    """.update.run

  override def findByEmail(email: Email): ConnectionIO[Option[User]] =
    (SelectQuery ++ fr"WHERE email = $email")
      .query[User]
      .option

  override def findById(userId: String): ConnectionIO[Option[User]] =
    (SelectQuery ++ fr"WHERE id = $userId")
      .query[User]
      .option
}
