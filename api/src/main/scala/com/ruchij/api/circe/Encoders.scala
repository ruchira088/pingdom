package com.ruchij.api.circe

import enumeratum.EnumEntry
import io.circe.{Encoder, Json}
import org.http4s.{Header, Headers, Method}
import org.joda.time.DateTime
import shapeless.{::, Generic, HNil}

import scala.concurrent.duration.FiniteDuration

object Encoders {

  implicit val dateTimeEncoder: Encoder[DateTime] = Encoder.encodeString.contramap[DateTime](_.toString)

  implicit val finiteDurationEncoder: Encoder[FiniteDuration] =
    Encoder.encodeString.contramap[FiniteDuration](_.toString())

  implicit val methodEncoder: Encoder[Method] = Encoder.encodeString.contramap[Method](_.name)

  implicit val headerEncoder: Encoder[Header] =
    (header: Header) => Json.obj("name" -> Json.fromString(header.name.value), "value" -> Json.fromString(header.value))

  implicit val headersEncoder: Encoder[Headers] =
    (headers: Headers) => Encoder.encodeList[Header].apply(headers.toList)

  implicit def enumEncoder[A <: EnumEntry]: Encoder[A] =
    Encoder.encodeString.contramap(_.entryName)

  implicit def throwableEncoder[A <: Throwable]: Encoder[A] =
    Encoder.encodeString.contramap[A](_.getMessage)

  implicit def stringValueClassEncoder[A <: AnyVal](implicit generic: Generic.Aux[A, String :: HNil]): Encoder[A] =
    valueClassEncoder[A, String]

  def valueClassEncoder[A <: AnyVal, R](implicit generic: Generic.Aux[A, R :: HNil], encoder: Encoder[R]): Encoder[A] =
    encoder.contramap[A] { value =>
      generic.to(value).head
    }

}
