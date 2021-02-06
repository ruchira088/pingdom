package com.ruchij.daos.user

import com.ruchij.daos.user.models.{Email, User}

trait UserDao[F[_]] {
  def save(user: User): F[Int]

  def findByEmail(email: Email): F[Option[User]]
}
