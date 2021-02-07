package com.ruchij.services.user

import com.ruchij.daos.user.models.{Email, User}
import com.ruchij.services.user.models.Password

trait UserService[F[_]] {
  def createNewUser(firstName: String, lastName: String, email: Email, password: Password): F[User]
}
