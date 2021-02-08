package com.ruchij.migration

import cats.effect.{ExitCode, IO, IOApp, Sync}
import cats.implicits._
import com.ruchij.config.{DatabaseConfiguration, MigrationConfiguration}
import com.ruchij.migration.db.DatabaseDriver
import liquibase.{Contexts, Liquibase}
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import pureconfig.ConfigSource

import java.sql.DriverManager

object MigrationApp extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      configObjectSource <- IO.delay(ConfigSource.defaultApplication)
      migrationApplicationConfiguration <- MigrationConfiguration.load[IO](configObjectSource)

    }
    yield ExitCode.Success


  def migrate[F[_]: Sync](databaseConfiguration: DatabaseConfiguration): F[Unit] =
    for {
      databaseDriver <- DatabaseDriver.from[F](databaseConfiguration.url)
      _ <- Sync[F].delay(Class.forName(databaseDriver.clazz.getName))

      connection <- Sync[F].delay {
        DriverManager.getConnection(
          databaseConfiguration.url,
          databaseConfiguration.username,
          databaseConfiguration.password
        )
      }

      database <- Sync[F].delay {
        DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection))
      }

      resourceAccessor <- Sync[F].delay(new ClassLoaderResourceAccessor())

      liquibase = new Liquibase("db-migrations/changelog.xml", resourceAccessor, database)

      _ <- Sync[F].delay(liquibase.update(new Contexts()))
      _ <- Sync[F].delay(liquibase.close())
    }
    yield (): Unit
}
