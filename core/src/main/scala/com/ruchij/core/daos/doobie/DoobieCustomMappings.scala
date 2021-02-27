package com.ruchij.core.daos.doobie

import doobie.util.{Get, Put}
import doobie.implicits.javasql._
import enumeratum.{Enum, EnumEntry}
import org.joda.time.DateTime
import shapeless.{Generic, HNil, ::}

import java.sql.Timestamp

object DoobieCustomMappings {

  implicit val dateTimePut: Put[DateTime] =
    Put[Timestamp].tcontramap[DateTime](dateTime => new Timestamp(dateTime.getMillis))

  implicit val dateTimeGet: Get[DateTime] = Get[Timestamp].map(timestamp => new DateTime(timestamp.getTime))

  implicit def enumPut[A <: EnumEntry]: Put[A] = Put[String].contramap[A](_.entryName)

  implicit def enumGet[A <: EnumEntry](implicit enumValues: Enum[A]): Get[A] =
    Get[String].temap { value =>
      enumValues.withNameInsensitiveEither(value).left.map(_.getMessage())
    }

  implicit def valueClassWrapperPut[A <: AnyVal, R](implicit generic: Generic.Aux[A, R :: HNil], put: Put[R]): Put[A] =
    put.contramap[A](value => generic.to(value).head)

  implicit def valueClassWrapperGet[A <: AnyVal, R](implicit generic: Generic.Aux[A, R :: HNil], get: Get[R]): Get[A] =
    get.tmap(value => generic.from(value :: HNil))

}
