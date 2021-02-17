package com.ruchij.config

import cats.{Applicative, ApplicativeError}
import pureconfig.ConfigObjectSource
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._

case class MigrationConfiguration(databaseConfiguration: DatabaseConfiguration)

object MigrationConfiguration {

  def load[F[_]: ApplicativeError[*[_], Throwable]](configObjectSource: ConfigObjectSource): F[MigrationConfiguration] =
    configObjectSource.load[MigrationConfiguration]
      .fold(
        failures => ApplicativeError[F, Throwable].raiseError(ConfigReaderException[MigrationConfiguration](failures)),
        config => Applicative[F].pure(config)
      )

}
