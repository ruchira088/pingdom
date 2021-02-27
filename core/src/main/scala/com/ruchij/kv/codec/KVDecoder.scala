package com.ruchij.kv.codec

import cats.effect.Sync
import cats.implicits._
import cats.{Applicative, Functor, Monad, MonadError}
import com.ruchij.syntax._
import org.joda.time.DateTime
import shapeless.{::, <:!<, Generic, HList, HNil}

trait KVDecoder[F[_], -A, B] {

  def decode(value: A): F[B]

}

object KVDecoder {

  def apply[F[_], A, B](implicit kvDecoder: KVDecoder[F, A, B]): KVDecoder[F, A, B] = kvDecoder

  implicit class KVDecoderWrapper[A](value: A) {
    def decode[F[_], B](implicit kvDecoder: KVDecoder[F, A, B]): F[B] = kvDecoder.decode(value)
  }

  implicit class KVDecoderOps[F[_], A, D](kvDecoder: KVDecoder[F, A, D]) {
    def flatMapF[B](f: D => F[B])(implicit monad: Monad[F]): KVDecoder[F, A, B] =
      (value: A) => kvDecoder.decode(value).flatMap(f).map(identity)
  }

  trait ItemLength[A] {
    val length: Int
  }

  object ItemLength {
    def apply[A](implicit itemLength: ItemLength[A]): ItemLength[A] = itemLength

    implicit def genericItemLength[A, Repr <: HList: Generic.Aux[A, *]](
      implicit reprItemLength: ItemLength[Repr]
    ): ItemLength[A] =
      new ItemLength[A] {
        override val length: Int = reprItemLength.length
      }

    implicit def hlistItemLength[H, T <: HList](
      implicit headItemLength: ItemLength[H],
      tailItemLength: ItemLength[T]
    ): ItemLength[H :: T] =
      new ItemLength[H :: T] {
        override val length: Int = headItemLength.length + tailItemLength.length
      }

    implicit def valueItemLength[A: * <:!< Product]: ItemLength[A] =
      new ItemLength[A] {
        override val length: Int = 1
      }

    implicit val hnilItemLength: ItemLength[HNil] = new ItemLength[HNil] {
      override val length: Int = 0
    }
  }

  implicit def stringKVDecoder[F[_]: Applicative]: KVDecoder[F, String, String] =
    (value: String) => Applicative[F].pure(value)

  implicit def stringNumericKVDecoder[F[_]: MonadError[*[_], Throwable], A: Numeric]: KVDecoder[F, String, A] =
    KVDecoder[F, String, String].flatMapF { string =>
      Numeric[A].parseString(string).toF[Throwable, F] {
        new IllegalStateException(s"Unable to parse $string as a number")
      }
    }

  implicit def stringDateTimeKVDecoder[F[_]: Sync]: KVDecoder[F, String, DateTime] =
    KVDecoder[F, String, String].flatMapF { string =>
      Sync[F].delay(DateTime.parse(string))
    }

  implicit def genericKVDecoder[F[_]: Functor, A, B, Repr <: HList](
    implicit generic: Generic.Aux[B, Repr],
    kvDecoder: KVDecoder[F, A, Repr]
  ): KVDecoder[F, A, B] =
    (value: A) => kvDecoder.decode(value).map(generic.from)

  implicit def hlistValueKVDecoder[F[_]: MonadError[*[_], Throwable], A: Consolidator, H, T <: HList](
    implicit headKVDecoder: KVDecoder[F, A, H],
    tailKVDecoder: KVDecoder[F, A, T],
    itemLength: ItemLength[H]
  ): KVDecoder[F, A, H :: T] =
    (value: A) =>
      for {
        (headA, tailA) <- Consolidator[A]
          .split(value, itemLength.length)
          .toF[Throwable, F](new IllegalStateException("Expected to be not empty"))

        head <- headKVDecoder.decode(headA)
        tail <- tailKVDecoder.decode(tailA)
      } yield head :: tail

  implicit def hnilKVDecoder[F[_]: MonadError[*[_], Throwable], A: Consolidator]: KVDecoder[F, A, HNil] =
    (value: A) =>
      Consolidator[A]
        .split(value, 1)
        .toEmptyF[Throwable, F] { value =>
          new IllegalStateException(s"Expected to be empty, but found: $value")
        }
        .productR(Applicative[F].pure(HNil))

}
