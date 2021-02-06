package com.ruchij.daos.doobie

import doobie.util.{Get, Put}
import doobie.implicits.javasql._
import org.joda.time.DateTime

import java.sql.Timestamp

object DoobieCustomMappings {
  implicit val dateTimePut: Put[DateTime] =
    Put[Timestamp].tcontramap[DateTime](dateTime => new Timestamp(dateTime.getMillis))

  implicit val dateTimeGet: Get[DateTime] = Get[Timestamp].map(timestamp => new DateTime(timestamp.getTime))
}
