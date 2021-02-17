package com.ruchij.web.routes

import cats.effect.Sync
import cats.implicits._
import com.ruchij.services.auth.AuthenticationService
import com.ruchij.web.requests.AuthenticationRequest
import com.ruchij.circe.Decoders._
import com.ruchij.circe.Encoders._
import org.http4s.HttpRoutes
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
        }
        yield response
    }
  }

}
