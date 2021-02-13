package com.ruchij.web.routes

import cats.effect.Sync
import cats.implicits._
import com.ruchij.circe.Decoders._
import com.ruchij.circe.Encoders._
import com.ruchij.daos.user.models.User
import com.ruchij.services.auth.AuthenticationService
import com.ruchij.services.user.UserService
import com.ruchij.web.middleware.Authenticator
import com.ruchij.web.requests.NewUserRequest
import com.ruchij.web.validator.Validator.{RequestWrapper, noValidator}
import io.circe.generic.auto._
import org.http4s.{ContextRoutes, HttpRoutes}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

object UserRoutes {
  def apply[F[_]: Sync](userService: UserService[F], authenticationService: AuthenticationService[F])(
    implicit http4sDsl: Http4sDsl[F]
  ): HttpRoutes[F] = {
    import http4sDsl._

    HttpRoutes.of[F] {
      case request @ POST -> Root =>
        for {
          NewUserRequest(firstName, lastName, email, password) <- request.to[NewUserRequest]

          user <- userService.createNewUser(firstName, lastName, email, password)

          response <- Created(user)
        } yield response
    } <+>
      Authenticator.routes[F](authenticationService).apply {
        ContextRoutes.of[User, F] {
          case GET -> Root as user => Ok(user)
        }
      }
  }
}
