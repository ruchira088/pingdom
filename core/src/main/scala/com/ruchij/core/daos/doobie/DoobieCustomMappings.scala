package com.ruchij.core.daos.doobie

import doobie.implicits.javasql._
import doobie.util.{Get, Put}
import enumeratum.{Enum, EnumEntry}
import org.http4s.{Method, Uri}
import org.joda.time.DateTime
import shapeless.{::, Generic, HNil}

import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object DoobieCustomMappings {

  implicit val dateTimePut: Put[DateTime] =
    Put[Timestamp].tcontramap[DateTime](dateTime => new Timestamp(dateTime.getMillis))

  implicit val dateTimeGet: Get[DateTime] = Get[Timestamp].map(timestamp => new DateTime(timestamp.getTime))

  implicit def enumPut[A <: EnumEntry]: Put[A] = Put[String].contramap[A](_.entryName)

  implicit def enumGet[A <: EnumEntry](implicit enumValues: Enum[A]): Get[A] =
    Get[String].temap { value =>
      enumValues.withNameInsensitiveEither(value).left.map(_.getMessage())
    }

  implicit val uriPut: Put[Uri] = Put[String].contramap(_.renderString)

  implicit val uriGet: Get[Uri] = Get[String].temap(value => Uri.fromString(value).left.map(_.message))

  implicit val methodPut: Put[Method] = Put[String].contramap(_.name)

  implicit val methodGet: Get[Method] = Get[String].temap(value => Method.fromString(value).left.map(_.message))

  implicit val finiteDurationPut: Put[FiniteDuration] = Put[Long].contramap(_.toMillis)

  implicit val finiteDurationGet: Get[FiniteDuration] =
    Get[Long].map(long => FiniteDuration(long, TimeUnit.MILLISECONDS))

  implicit def valueClassWrapperPut[A <: AnyVal, R](implicit generic: Generic.Aux[A, R :: HNil], put: Put[R]): Put[A] =
    put.contramap[A](value => generic.to(value).head)

  implicit def valueClassWrapperGet[A <: AnyVal, R](implicit generic: Generic.Aux[A, R :: HNil], get: Get[R]): Get[A] =
    get.tmap(value => generic.from(value :: HNil))

}
