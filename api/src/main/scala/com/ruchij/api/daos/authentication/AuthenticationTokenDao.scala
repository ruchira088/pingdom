package com.ruchij.api.daos.authentication

import com.ruchij.api.daos.authentication.models.AuthenticationToken

trait AuthenticationTokenDao[F[_]] {

  def save(authenticationToken: AuthenticationToken): F[Int]

  def findByUserIdAndSecret(userId: String, secret: String): F[Option[AuthenticationToken]]

  def removeByUserIdAndSecret(userId: String, secret: String): F[Option[AuthenticationToken]]

}
