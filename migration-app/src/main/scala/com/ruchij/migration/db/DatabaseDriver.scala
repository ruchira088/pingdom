package com.ruchij.migration.db

import cats.{Applicative, ApplicativeError}
import com.ruchij.migration.db.DatabaseDriver.DbUrl
import enumeratum.{Enum, EnumEntry}
import org.h2.{Driver => H2Driver}
import org.postgresql.{Driver => PostgresqlDriver}

import java.sql.Driver
import scala.reflect.{ClassTag, classTag}
import scala.util.matching.Regex

sealed abstract class DatabaseDriver[A <: Driver: ClassTag] extends EnumEntry {

  val prefix: String

  def supports(url: String): Boolean =
    url match {
      case DbUrl(driver) => prefix.equalsIgnoreCase(driver)

      case _ => false
    }

  val clazz: Class[_] = classTag[A].runtimeClass

}

object DatabaseDriver extends Enum[DatabaseDriver[_]] {

  val DbUrl: Regex = "jdbc:(\\w+):.*".r

  case object Postgresql extends DatabaseDriver[PostgresqlDriver] {
    override val prefix: String = "postgresql"
  }

  case object H2 extends DatabaseDriver[H2Driver] {
    override val prefix: String = "h2"
  }

  override def values: IndexedSeq[DatabaseDriver[_]] = findValues

  def from[F[_]: ApplicativeError[*[_], Throwable]](url: String): F[DatabaseDriver[_]] =
    values
      .find(_.supports(url))
      .fold[F[DatabaseDriver[_]]](
        ApplicativeError[F, Throwable].raiseError(new Exception(s"Unable to determine database driver from $url"))
      ) { driver =>
        Applicative[F].pure(driver)
      }

}
