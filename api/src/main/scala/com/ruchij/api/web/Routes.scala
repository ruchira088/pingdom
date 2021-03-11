package com.ruchij.api.web

import cats.effect.Sync
import com.ruchij.api.services.health.HealthService
import com.ruchij.api.services.auth.AuthenticationService
import com.ruchij.api.services.user.UserService
import com.ruchij.api.web.middleware.{ExceptionHandler, NotFoundHandler}
import com.ruchij.api.web.routes.{AuthenticationRoutes, HealthRoutes, PingRoutes, UserRoutes}
import com.ruchij.core.services.ping.PingService
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

object Routes {

  def apply[F[_]: Sync](
    userService: UserService[F],
    pingService: PingService[F],
    authenticationService: AuthenticationService[F],
    healthService: HealthService[F]
  ): HttpApp[F] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}

    val routes: HttpRoutes[F] =
      Router(
        "/service" -> HealthRoutes(healthService),
        "/user" -> UserRoutes(userService, authenticationService),
        "/authentication" -> AuthenticationRoutes(authenticationService),
        "/ping" -> PingRoutes(pingService, authenticationService)
      )

    ExceptionHandler {
      NotFoundHandler(routes)
    }
  }

}
