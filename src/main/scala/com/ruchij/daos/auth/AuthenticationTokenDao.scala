package com.ruchij.daos.auth

import com.ruchij.daos.auth.models.AuthenticationToken

trait AuthenticationTokenDao[F[_]] {

  def save(authenticationToken: AuthenticationToken): F[Int]

  def findByUserIdAndSecret(userId: String, secret: String): F[Option[AuthenticationToken]]

}
