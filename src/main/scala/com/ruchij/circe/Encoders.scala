package com.ruchij.circe

import io.circe.Encoder
import org.joda.time.DateTime
import shapeless.{::, Generic, HNil}

object Encoders {

  implicit val dateTimeEncoder: Encoder[DateTime] = Encoder.encodeString.contramap[DateTime](_.toString)

  implicit def throwableEncoder[A <: Throwable]: Encoder[A] =
    Encoder.encodeString.contramap[A](_.getMessage)

  implicit def stringValueClassEncoder[A <: AnyVal](implicit generic: Generic.Aux[A, String :: HNil]): Encoder[A] =
    valueClassEncoder[A, String]

  def valueClassEncoder[A <: AnyVal, R](
    implicit generic: Generic.Aux[A, R :: HNil],
    encoder: Encoder[R]
  ): Encoder[A] =
    encoder.contramap[A] { value => generic.to(value).head }

}
