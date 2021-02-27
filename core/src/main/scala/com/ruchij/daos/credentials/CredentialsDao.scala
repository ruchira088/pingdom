package com.ruchij.daos.credentials

import com.ruchij.daos.credentials.models.Credentials

trait CredentialsDao[F[_]] {

  def save(credentials: Credentials): F[Int]

  def findByUserId(userId: String): F[Option[Credentials]]

}
