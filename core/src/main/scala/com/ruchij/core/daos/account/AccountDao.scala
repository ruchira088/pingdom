package com.ruchij.core.daos.account

import com.ruchij.core.daos.account.models.Account

trait AccountDao[F[_]] {

  def save(account: Account): F[Int]

  def findById(id: String): F[Option[Account]]

}
