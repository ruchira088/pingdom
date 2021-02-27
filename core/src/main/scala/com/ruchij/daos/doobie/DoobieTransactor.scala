package com.ruchij.daos.doobie

import cats.effect.{Async, ContextShift}
import cats.implicits._
import com.ruchij.config.DatabaseConfiguration
import com.ruchij.types.CustomBlocker.IOBlocker
import doobie.Transactor
import doobie.util.transactor.Transactor.Aux

object DoobieTransactor {

  def create[F[_]: Async: ContextShift](databaseConfiguration: DatabaseConfiguration, blocker: IOBlocker): F[Aux[F, Unit]] =
    DatabaseDriver.from[F](databaseConfiguration.url)
      .map { databaseDriver =>
        Transactor.fromDriverManager(
          databaseDriver.clazz.getName,
          databaseConfiguration.url,
          databaseConfiguration.username,
          databaseConfiguration.password,
          blocker
        )
      }

}
