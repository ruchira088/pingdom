package com.ruchij.kv.codec

import cats.{Applicative, Monad, Semigroup}
import cats.implicits._
import cats.kernel.Monoid
import org.joda.time.DateTime
import shapeless.{::, Generic, HList, HNil}

trait KVEncoder[F[_], -A, +B] {
  def encode[C >: B](value: A): F[C]
}

object KVEncoder {
  def apply[F[_], A, B](implicit kvEncoder: KVEncoder[F, A, B]): KVEncoder[F, A, B] = kvEncoder

  implicit val stringMonoid: Monoid[String] =
    new Monoid[String] {
      override def empty: String = ""

      override def combine(x: String, y: String): String =
        if (x.isEmpty) y else if (y.isEmpty) x else x + ":::" + y
  }

  implicit class KVEncoderValueOp[A](value: A) {
    def encode[F[_], B](implicit kvEncoder: KVEncoder[F, A, B]): F[B] = kvEncoder.encode(value)
  }

  implicit class KVEncoderOps[F[_], A, D](kvEncoder: KVEncoder[F, A, D]) {
    def comap[B](f: B => A): KVEncoder[F, B, D] =
      new KVEncoder[F, B, D] {
        override def encode[C >: D](value: B): F[C] =
          kvEncoder.encode(f(value))
      }
  }

  implicit def stringKVEncoder[F[_]: Applicative]: KVEncoder[F, String, String] =
    new KVEncoder[F, String, String] {
      override def encode[C >: String](value: String): F[C] = Applicative[F].pure(value)
    }

  implicit def numericStringKVEncoder[F[_]: Applicative, A: Numeric]: KVEncoder[F, A, String] =
    KVEncoder[F, String, String].comap(_.toString)

  implicit def dateTimeKVEncoder[F[_]: Applicative]: KVEncoder[F, DateTime, String] =
    KVEncoder[F, String, String].comap(_.getMillis.toString)

  implicit def genericKVEncoder[F[_], A, B, Repr <: HList](
    implicit generic: Generic.Aux[A, Repr],
    kvEncoder: KVEncoder[F, Repr, B]
  ): KVEncoder[F, A, B] =
    new KVEncoder[F, A, B] {
      override def encode[C >: B](value: A): F[C] =
        kvEncoder.encode(generic.to(value))
    }

  implicit def hlistKVEncoder[F[_]: Monad, H, T <: HList, B: Semigroup](
    implicit headKvEncoder: KVEncoder[F, H, B],
    tailKvEncoder: KVEncoder[F, T, B]
  ): KVEncoder[F, H :: T, B] =
    new KVEncoder[F, H :: T, B] {
      override def encode[C >: B](value: H :: T): F[C] =
        for {
          head <- headKvEncoder.encode(value.head)
          tail <- tailKvEncoder.encode(value.tail)
        }
        yield Semigroup[B].combine(head, tail)
    }

  implicit def hnilKVEncoder[F[_]: Applicative, B: Monoid]: KVEncoder[F, HNil, B] =
    new KVEncoder[F, HNil, B] {
      override def encode[C >: B](value: HNil): F[C] =
        Applicative[F].pure(Monoid[B].empty)
    }
}
