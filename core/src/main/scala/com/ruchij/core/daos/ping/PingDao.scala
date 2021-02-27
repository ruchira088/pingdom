package com.ruchij.core.daos.ping

import com.ruchij.core.daos.ping.models.Ping

trait PingDao[F[_]] {
  def save(ping: Ping): F[Int]

  def findByAccount(accountId: String): F[List[Ping]]

  def findById(pingId: String): F[Option[Ping]]
}
