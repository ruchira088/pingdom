package com.ruchij.services.hash

trait PasswordHashingService[F[_]] {

  def hash(input: String): F[String]

  def checkPassword(input: String, saltedPasswordHash: String): F[Boolean]

}
