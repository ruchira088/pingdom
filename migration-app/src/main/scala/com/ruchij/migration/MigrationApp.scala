package com.ruchij.migration

import cats.effect.{ExitCode, IO, IOApp, Sync}
import cats.implicits._
import com.ruchij.config.{DatabaseConfiguration, MigrationConfiguration}
import org.flywaydb.core.Flyway
import pureconfig.ConfigSource

object MigrationApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      configObjectSource <- IO.delay(ConfigSource.defaultApplication)
      migrationApplicationConfiguration <- MigrationConfiguration.load[IO](configObjectSource)

      _ <- migrate[IO](migrationApplicationConfiguration.databaseConfiguration)
    }
    yield ExitCode.Success


  def migrate[F[_]: Sync](databaseConfiguration: DatabaseConfiguration): F[Unit] =
    for {
      flyway <- Sync[F].delay {
        Flyway.configure()
          .dataSource(
            databaseConfiguration.url,
            databaseConfiguration.username,
            databaseConfiguration.password
          )
          .load()
      }

      result <- Sync[F].delay(flyway.migrate())
    }
    yield (): Unit

}
