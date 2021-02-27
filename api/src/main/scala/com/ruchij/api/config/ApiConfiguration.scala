package com.ruchij.api.config

import cats.ApplicativeError
import com.ruchij.migration.config.DatabaseConfiguration
import com.ruchij.core.config.{AuthenticationConfiguration, BuildInformation, RedisConfiguration}
import com.ruchij.core.config.ConfigReaders._
import com.ruchij.core.types.FunctionKTypes.eitherToF
import pureconfig.ConfigObjectSource
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._

case class ApiConfiguration(
  databaseConfiguration: DatabaseConfiguration,
  httpConfiguration: HttpConfiguration,
  authenticationConfiguration: AuthenticationConfiguration,
  redisConfiguration: RedisConfiguration,
  buildInformation: BuildInformation
)

object ApiConfiguration {

  def parse[F[_]: ApplicativeError[*[_], Throwable]](configObjectSource: ConfigObjectSource): F[ApiConfiguration] =
    eitherToF.apply {
      configObjectSource.load[ApiConfiguration].left.map(ConfigReaderException.apply)
    }

}
