package com.ruchij.core.daos.auth

import cats.Functor
import cats.implicits._
import com.ruchij.core.daos.auth.models.AuthenticationToken
import com.ruchij.core.kv.codec.Consolidator
import com.ruchij.core.kv.KeyValueStore

class AuthenticationTokenKeyValueStore[F[_]: Functor](keyValueStore: KeyValueStore[F, String, AuthenticationToken])
    extends AuthenticationTokenDao[F] {

  override def save(authenticationToken: AuthenticationToken): F[Int] =
    keyValueStore
      .put(Consolidator[String].combine(authenticationToken.userId, authenticationToken.secret), authenticationToken)
      .map(_.fold(1)(_ => 2))

  override def findByUserIdAndSecret(userId: String, secret: String): F[Option[AuthenticationToken]] =
    keyValueStore.get(Consolidator[String].combine(userId, secret))

  override def removeByUserIdAndSecret(userId: String, secret: String): F[Option[AuthenticationToken]] =
    keyValueStore.delete(Consolidator[String].combine(userId, secret))
}
