package com.ruchij.api.web.middleware

import cats.data.{Kleisli, OptionT}
import cats.{ApplicativeError, MonadError}
import com.ruchij.core.daos.user.models.User
import com.ruchij.core.exceptions.AuthenticationException
import com.ruchij.core.services.auth.AuthenticationService
import org.http4s.Credentials.Token
import org.http4s.headers.Authorization
import org.http4s.server.ContextMiddleware
import org.http4s.{AuthScheme, ContextRequest, Request, Response}

import scala.util.matching.Regex

object Authenticator {

  case class BearerCredentials(userId: String, secret: String)

  val UserCredentials: Regex = "(\\S+):(\\S+)".r

  def bearerCredentials[F[_]](request: Request[F]): Option[BearerCredentials] =
    request.headers
      .get(Authorization)
      .collect {
        case Authorization(Token(AuthScheme.Bearer, UserCredentials(userId, secret))) =>
          BearerCredentials(userId, secret)
      }

  def routes[F[_]: MonadError[*[_], Throwable]](
    authenticationService: AuthenticationService[F]
  ): ContextMiddleware[F, User] =
    authenticatedRoutes =>
      Kleisli[OptionT[F, *], Request[F], Response[F]] { request =>
        bearerCredentials(request)
          .fold[OptionT[F, Response[F]]](OptionT.liftF {
            ApplicativeError[F, Throwable].raiseError(AuthenticationException("Missing authentication credentials"))
          }) {
            case BearerCredentials(userId, secret) =>
              OptionT
                .liftF(authenticationService.authenticate(userId, secret))
                .flatMap { user =>
                  authenticatedRoutes.run(ContextRequest(user, request))
                }
          }
    }

  object withCredentials {
    def unapply[F[_]](request: Request[F]): Option[(Request[F], BearerCredentials)] =
      Authenticator.bearerCredentials(request).map(request -> _)
  }

}
