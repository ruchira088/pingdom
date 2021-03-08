package com.ruchij.api.services.auth

import com.ruchij.api.daos.authentication.models.AuthenticationToken
import com.ruchij.core.daos.user.models.{Email, User}

trait AuthenticationService[F[_]] {

  def login(email: Email, password: String): F[AuthenticationToken]

  def authenticate(userId: String, secret: String): F[User]

  def logout(userId: String, secret: String): F[AuthenticationToken]

}
