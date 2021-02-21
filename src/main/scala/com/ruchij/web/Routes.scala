package com.ruchij.web

import cats.effect.Sync
import com.ruchij.services.auth.AuthenticationService
import com.ruchij.services.health.HealthService
import com.ruchij.services.user.UserService
import com.ruchij.web.middleware.{ExceptionHandler, NotFoundHandler}
import com.ruchij.web.routes.{HealthRoutes, UserRoutes}
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

object Routes {

  def apply[F[_]: Sync](userService: UserService[F], authenticationService: AuthenticationService[F], healthService: HealthService[F]): HttpApp[F] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}

    val routes: HttpRoutes[F] =
      Router(
        "/service" -> HealthRoutes(healthService),
        "/user" -> UserRoutes(userService, authenticationService)
      )

    ExceptionHandler {
      NotFoundHandler(routes)
    }
  }

}
