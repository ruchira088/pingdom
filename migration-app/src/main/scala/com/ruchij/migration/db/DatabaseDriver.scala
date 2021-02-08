package com.ruchij.migration.db

import cats.{Applicative, ApplicativeError}
import com.ruchij.migration.db.DatabaseDriver.DbUrl
import enumeratum.{Enum, EnumEntry}
import org.postgresql.{Driver => PostgresqlDriver}

import java.sql.Driver
import scala.util.matching.Regex

sealed trait DatabaseDriver[A <: Driver] extends EnumEntry {
  val prefix: String

  def supports(url: String): Boolean =
    url match {
      case DbUrl(driver) => prefix.equalsIgnoreCase(driver)

      case _ => false
    }

  val clazz: Class[A]
}

object DatabaseDriver extends Enum[DatabaseDriver[_]] {
  val DbUrl: Regex = "jdbc:(\\S+)://.*".r

  case object Postgresql extends DatabaseDriver[PostgresqlDriver] {
    override val prefix: String = "postgresql"

    override val clazz: Class[PostgresqlDriver] = classOf[PostgresqlDriver]
  }

  override def values: IndexedSeq[DatabaseDriver[_]] = findValues

  def from[F[_]: ApplicativeError[*[_], Throwable]](url: String): F[DatabaseDriver[_]] =
    values
      .find(_.supports(url))
      .fold[F[DatabaseDriver[_]]](
        ApplicativeError[F, Throwable].raiseError(new Exception("Unable to determine database driver"))
      ) { driver =>
        Applicative[F].pure(driver)
      }
}
