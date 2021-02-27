package com.ruchij.api.services.user

import com.ruchij.core.daos.user.models.{Email, User}
import com.ruchij.api.services.user.models.Password

trait UserService[F[_]] {

  def createNewUser(firstName: String, lastName: String, email: Email, password: Password): F[User]

}
