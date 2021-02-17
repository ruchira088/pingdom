package com.ruchij.daos.account

import com.ruchij.daos.account.models.Account
import com.ruchij.daos.doobie.DoobieCustomMappings.{dateTimeGet, dateTimePut}
import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator

object DoobieAccountDao extends AccountDao[ConnectionIO] {

  override def save(account: Account): ConnectionIO[Int] =
    sql"""
      INSERT INTO account (id, created_at, modified_at, name)
        VALUES (${account.id}, ${account.createdAt}, ${account.modifiedAt}, ${account.name})
    """
      .update
      .run

  override def findById(id: String): ConnectionIO[Option[Account]] =
    sql"SELECT id, created_at, modified_at, name FROM account WHERE id = $id".query[Account].option

}
