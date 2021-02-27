package com.ruchij.core.daos.credentials

import com.ruchij.core.daos.credentials.models.Credentials

trait CredentialsDao[F[_]] {

  def save(credentials: Credentials): F[Int]

  def findByUserId(userId: String): F[Option[Credentials]]

}
