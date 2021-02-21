package com.ruchij.test

import cats.Monad
import org.http4s.{EntityEncoder, Header, Headers, Method, Request, Uri}
import org.http4s.Method.PermitsBody
import org.http4s.headers.`Content-Length`

package object syntax {
  implicit class RequestOps(val method: Method with PermitsBody) {
    def apply[F[_]: Monad, A](uri: Uri, body: A, headers: Header*)(implicit entityEncoder: EntityEncoder[F, A]): Request[F] = {
      val entity = entityEncoder.toEntity(body)
      val contentLengthHeader: Option[`Content-Length`] =
        entity.length.flatMap { length => `Content-Length`.fromLong(length).toOption }

      Request(
        method = method,
        uri = uri,
        body = entity.body,
        headers = entityEncoder.headers ++ Headers.of(headers: _*) ++ Headers(contentLengthHeader.toList)
      )
    }

  }
}
