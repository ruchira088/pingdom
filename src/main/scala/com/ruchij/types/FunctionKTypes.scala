package com.ruchij.types

import cats.effect.Bracket
import cats.{Applicative, ApplicativeError, ~>}
import doobie.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor

object FunctionKTypes {

  implicit def eitherToF[L, F[_]: ApplicativeError[*[_], L]]: Either[L, *] ~> F =
    new ~>[Either[L, *], F] {
      override def apply[A](either: Either[L, A]): F[A] =
        either.fold(ApplicativeError[F, L].raiseError, Applicative[F].pure)
    }

  def connectionIoToF[F[_]: Bracket[*[_], Throwable]](transactor: Transactor.Aux[F, Unit]): ConnectionIO ~> F =
    new ~>[ConnectionIO, F] {
      override def apply[A](connectionIO: ConnectionIO[A]): F[A] = connectionIO.transact(transactor)
    }

  def identityFunctionK[F[_]]: F ~> F = new ~>[F, F] {
    override def apply[A](fa: F[A]): F[A] = fa
  }
}
