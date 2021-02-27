package com.ruchij

import cats.{Applicative, ApplicativeError, MonadError}
import cats.implicits._
import com.ruchij.types.CoFunctor

package object syntax {

  implicit class OptionWrapper[A](option: Option[A]) {
    def toF[B, F[_]: ApplicativeError[*[_], B]](onEmpty: => B): F[A] =
      option.fold[F[A]](ApplicativeError[F, B].raiseError(onEmpty))(Applicative[F].pure)

    def toEmptyF[B, F[_]: ApplicativeError[*[_], B]](f: A => B): F[Unit] =
      option.fold(Applicative[F].unit) { value => ApplicativeError[F, B].raiseError(f(value)) }
  }

  implicit class FOptionOps[F[_], A](value: F[Option[A]]) {
    def toF[B](onEmpty: => B)(implicit monadError: MonadError[F, B]): F[A] =
      value.flatMap(_.toF[B, F](onEmpty))
  }

  implicit class CoFunctorOps[F[_], A](value: F[A]) {
    def comap[B](f: B => A)(implicit coFunctor: CoFunctor[F]): F[B] =
      coFunctor.comap(value)(f)
  }

}