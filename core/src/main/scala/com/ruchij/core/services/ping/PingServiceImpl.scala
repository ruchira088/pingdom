package com.ruchij.core.services.ping

import cats.effect.Sync
import cats.implicits._
import cats.~>
import com.ruchij.core.daos.permission.models.PermissionType
import com.ruchij.core.daos.ping.PingDao
import com.ruchij.core.daos.ping.models.Ping
import com.ruchij.core.services.authorization.AuthorizationService
import com.ruchij.core.types.{JodaClock, RandomGenerator}
import org.http4s.{Header, Headers, Method, Uri}

import java.util.UUID
import scala.concurrent.duration.FiniteDuration

class PingServiceImpl[F[_]: JodaClock: Sync, T[_]](authorizationService: AuthorizationService[F], pingDao: PingDao[T])(
  implicit transaction: T ~> F
) extends PingService[F] {

  override def insert(
    userId: String,
    accountId: String,
    method: Method,
    uri: Uri,
    headers: List[Header],
    body: Option[String],
    frequency: FiniteDuration
  ): F[Ping] =
    authorizationService.withPermission(userId, accountId, PermissionType.ReadWrite) {
      for {
        id <- RandomGenerator[F, UUID].generate.map(_.toString)
        timestamp <- JodaClock[F].currentTimestamp

        ping = Ping(id, accountId, timestamp, timestamp, uri, method, Headers(headers), body, frequency)

        _ <- transaction { pingDao.save(ping) }
      } yield ping
    }

}
