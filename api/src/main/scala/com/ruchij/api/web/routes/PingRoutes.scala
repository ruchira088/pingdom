package com.ruchij.api.web.routes

import cats.effect.Sync
import cats.implicits._
import com.ruchij.api.services.auth.AuthenticationService
import com.ruchij.api.web.middleware.Authenticator
import com.ruchij.api.web.requests.NewPingRequest
import com.ruchij.core.daos.user.models.User
import com.ruchij.core.services.ping.PingService
import com.ruchij.api.circe.Decoders._
import com.ruchij.api.circe.Encoders._
import org.http4s.circe.{decodeUri, encodeUri}
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import org.http4s.{ContextRoutes, HttpRoutes}

object PingRoutes {

  def apply[F[_]: Sync](pingService: PingService[F], authenticationService: AuthenticationService[F])(
    implicit http4sDsl: Http4sDsl[F]
  ): HttpRoutes[F] = {
    import http4sDsl._

    Authenticator.routes[F](authenticationService).apply {
      ContextRoutes.of[User, F] {

        case (request @ POST -> Root / accountId) as user =>
          for {
            NewPingRequest(uri, method, headers, body, frequency) <- request.as[NewPingRequest]

            ping <- pingService.insert(user.id, accountId, method, uri, headers, body, frequency)

            response <- Created(ping)
          } yield response
      }
    }

  }

}
