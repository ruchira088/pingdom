package com.ruchij.core.services.ping

import com.ruchij.core.daos.ping.models.Ping
import org.http4s.{Header, Method, Uri}

import scala.concurrent.duration.FiniteDuration

trait PingService[F[_]] {

  def insert(
    userId: String,
    accountId: String,
    method: Method,
    uri: Uri,
    headers: List[Header],
    body: Option[String],
    frequency: FiniteDuration
  ): F[Ping]

}
