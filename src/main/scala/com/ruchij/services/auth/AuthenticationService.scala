package com.ruchij.services.auth

import com.ruchij.daos.auth.models.AuthenticationToken
import com.ruchij.daos.user.models.{Email, User}

trait AuthenticationService[F[_]] {
  def login(email: Email, password: String): F[AuthenticationToken]

  def authenticate(userId: String, secret: String): F[User]
}