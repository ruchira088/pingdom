package com.ruchij.types

trait CoFunctor[F[_]] {

  def comap[A, B](fa: F[A])(f: B => A): F[B]

}