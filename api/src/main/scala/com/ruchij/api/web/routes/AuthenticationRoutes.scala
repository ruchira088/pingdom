package com.ruchij.api.web.routes

import cats.effect.Sync
import cats.implicits._
import com.ruchij.api.services.auth.AuthenticationService
import com.ruchij.api.web.requests.AuthenticationRequest
import com.ruchij.api.circe.Decoders._
import com.ruchij.api.circe.Encoders._
import com.ruchij.core.daos.user.models.User
import com.ruchij.api.web.middleware.Authenticator
import com.ruchij.api.web.middleware.Authenticator.{BearerCredentials, withCredentials}
import org.http4s.{ContextRoutes, HttpRoutes}
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

object AuthenticationRoutes {

  def apply[F[_]: Sync](authenticationService: AuthenticationService[F])(implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._

    HttpRoutes.of[F] {
      case request @ POST -> Root =>
        for {
          AuthenticationRequest(email, password) <- request.as[AuthenticationRequest]

          authenticationToken <- authenticationService.login(email, password)

          response <- Created(authenticationToken)
        } yield response
    } <+>
      Authenticator.routes(authenticationService).apply {
        ContextRoutes.of[User, F] {
          case DELETE -> Root withCredentials BearerCredentials(userId, secret) as _ =>
            authenticationService
              .logout(userId, secret)
              .flatMap(authenticationToken => Ok(authenticationToken))
        }
      }
  }

}
