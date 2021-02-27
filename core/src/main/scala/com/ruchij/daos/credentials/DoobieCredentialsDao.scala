package com.ruchij.daos.credentials

import com.ruchij.daos.doobie.DoobieCustomMappings.{dateTimeGet, dateTimePut}
import com.ruchij.daos.credentials.models.Credentials
import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator

object DoobieCredentialsDao extends CredentialsDao[ConnectionIO] {

  override def save(credentials: Credentials): ConnectionIO[Int] =
    sql"""
      INSERT INTO credentials (user_id, created_at, modified_at, salted_password_hash)
        VALUES (
          ${credentials.userId},
          ${credentials.createdAt},
          ${credentials.modifiedAt},
          ${credentials.saltedPasswordHash}
        )
    """
      .update
      .run

  override def findByUserId(userId: String): ConnectionIO[Option[Credentials]] =
    sql"SELECT user_id, created_at, modified_at, salted_password_hash FROM credentials WHERE user_id = $userId"
      .query[Credentials]
      .option

}
