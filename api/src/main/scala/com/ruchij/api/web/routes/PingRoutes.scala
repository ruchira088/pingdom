package com.ruchij.api.web.routes

import cats.effect.Sync
import cats.implicits._
import com.ruchij.api.circe.Decoders._
import com.ruchij.api.services.auth.AuthenticationService
import com.ruchij.api.web.middleware.Authenticator
import com.ruchij.api.web.requests.NewPingRequest
import com.ruchij.core.daos.user.models.User
import io.circe.generic.auto._
import org.http4s.{ContextRoutes, HttpRoutes}
import org.http4s.circe._
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
import org.http4s.dsl.Http4sDsl

object PingRoutes {

  def apply[F[_]: Sync](
    authenticationService: AuthenticationService[F]
  )(implicit http4sDsl: Http4sDsl[F]): HttpRoutes[F] = {
    import http4sDsl._

    Authenticator.routes[F](authenticationService).apply {
      ContextRoutes.of[User, F] {

        case (request @ POST -> Root) as user =>
          for {
            newPingRequest <- request.as[NewPingRequest]
          } yield ???
      }
    }

  }

}
