package com.ruchij.api.test

import cats.effect.{Blocker, Concurrent, ContextShift, Resource, Sync, Timer}
import com.ruchij.api.ApiApp
import com.ruchij.api.config.{ApiConfiguration, HttpConfiguration}
import com.ruchij.core.config.{AuthenticationConfiguration, BuildInformation, RedisConfiguration}
import com.ruchij.core.test.{availablePort, h2DatabaseConfiguration, startEmbeddedRedis}
import com.ruchij.core.types.CustomBlocker.{CpuBlocker, IOBlocker}
import com.ruchij.core.types.RandomGenerator
import com.ruchij.migration.MigrationApp
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.Stdout.instance
import org.http4s.HttpApp

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object HttpTestResource {

  val DefaultBuildInformation: BuildInformation = BuildInformation(Some("test-branch"), Some("my-commit"), None)

  val DefaultAuthenticationConfiguration: AuthenticationConfiguration =
    AuthenticationConfiguration(FiniteDuration(30, TimeUnit.SECONDS))

  val DefaultHttpConfiguration: HttpConfiguration = HttpConfiguration("0.0.0.0", 8000)

  def apiConfiguration[F[_]: Sync]: Resource[F, ApiConfiguration] =
    for {
      redisPort <- Resource.eval(availablePort[F](6300))
      _ <- startEmbeddedRedis[F](redisPort)

      databaseName <- Resource.eval(RandomGenerator[F, UUID].generate).map(_.toString)

      apiConfiguration = ApiConfiguration(
        h2DatabaseConfiguration(databaseName),
        DefaultHttpConfiguration,
        DefaultAuthenticationConfiguration,
        RedisConfiguration("localhost", redisPort, None),
        DefaultBuildInformation
      )

      _ <- Resource.eval(MigrationApp.migrate(apiConfiguration.databaseConfiguration))
    } yield apiConfiguration

  def apply[F[_]: Concurrent: ContextShift: Timer](
    apiConfiguration: ApiConfiguration
  )(implicit executionContext: ExecutionContext): Resource[F, HttpApp[F]] =
    Redis[F]
      .utf8(apiConfiguration.redisConfiguration.url)
      .evalMap { redisCommands =>
        ApiApp.program[F](
          IOBlocker(Blocker.liftExecutionContext(executionContext)),
          CpuBlocker(Blocker.liftExecutionContext(executionContext)),
          redisCommands,
          apiConfiguration
        )
      }

  def apply[F[_]: Concurrent: ContextShift: Timer](
    implicit executionContext: ExecutionContext
  ): Resource[F, HttpApp[F]] = apiConfiguration[F].flatMap(apply[F])
}
