package com.ruchij.daos.account

import com.ruchij.daos.account.models.Account

trait AccountDao[F[_]] {
  def save(account: Account): F[Int]

  def findById(id: String): F[Option[Account]]
}
