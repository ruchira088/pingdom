package com.ruchij.types

import cats.Functor
import cats.effect.Clock
import cats.implicits._
import org.joda.time.DateTime

import java.util.concurrent.TimeUnit

trait JodaClock[F[_]] {
  val currentTimestamp: F[DateTime]
}

object JodaClock {
  def apply[F[_]](implicit jodaClock: JodaClock[F]): JodaClock[F] = jodaClock

  implicit def fromClock[F[_]: Clock: Functor]: JodaClock[F] =
    new JodaClock[F] {
      override val currentTimestamp: F[DateTime] =
        Clock[F].realTime(TimeUnit.MILLISECONDS).map(timestamp => new DateTime(timestamp))
    }
}
