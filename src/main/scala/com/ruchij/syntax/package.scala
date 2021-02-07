package com.ruchij

import cats.{Applicative, ApplicativeError}

package object syntax {
  implicit class OptionWrapper[A](option: Option[A]) {
    def toF[B, F[_]: ApplicativeError[*[_], B]](onEmpty: => B): F[A] =
      option.fold[F[A]](ApplicativeError[F, B].raiseError(onEmpty))(Applicative[F].pure)

    def toEmptyF[B, F[_]: ApplicativeError[*[_], B]](f: A => B): F[Unit] =
      option.fold(Applicative[F].unit) { value => ApplicativeError[F, B].raiseError(f(value)) }
  }
}