package com.ruchij.core.daos.user

import com.ruchij.core.daos.user.models.{Email, User}

trait UserDao[F[_]] {

  def save(user: User): F[Int]

  def findByEmail(email: Email): F[Option[User]]

  def findById(userId: String): F[Option[User]]

}
