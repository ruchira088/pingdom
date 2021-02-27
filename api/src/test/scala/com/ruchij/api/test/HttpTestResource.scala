package com.ruchij.api.test

import cats.MonadError
import cats.effect.{Blocker, Concurrent, ContextShift, Resource, Sync, Timer}
import cats.implicits._
import com.ruchij.api.ApiApp
import com.ruchij.api.config.{ApiConfiguration, HttpConfiguration}
import com.ruchij.core.config.{AuthenticationConfiguration, BuildInformation, RedisConfiguration}
import com.ruchij.core.types.CustomBlocker.{CpuBlocker, IOBlocker}
import com.ruchij.core.types.RandomGenerator
import com.ruchij.migration.MigrationApp
import com.ruchij.migration.config.DatabaseConfiguration
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.Stdout.instance
import org.http4s.HttpApp
import redis.embedded.RedisServer

import java.net.ServerSocket
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object HttpTestResource {

  val DefaultBuildInformation: BuildInformation = BuildInformation(Some("test-branch"), Some("my-commit"), None)

  val DefaultAuthenticationConfiguration: AuthenticationConfiguration =
    AuthenticationConfiguration(FiniteDuration(30, TimeUnit.SECONDS))

  val DefaultHttpConfiguration: HttpConfiguration = HttpConfiguration("0.0.0.0", 8000)

  def startEmbeddedRedis[F[_]: Sync](port: Int): Resource[F, RedisServer] =
    Resource
      .pure[F, RedisServer](RedisServer.builder().port(port).build())
      .flatTap { redisServer =>
        Resource.make(Sync[F].delay(redisServer.start()))(_ => Sync[F].delay(redisServer.stop()))
      }

  def h2DatabaseConfiguration(databaseName: String): DatabaseConfiguration =
    DatabaseConfiguration(
      s"jdbc:h2:mem:$databaseName;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
      "",
      ""
    )

  def availablePort[F[_]: Sync](start: Int): F[Int] =
    RandomGenerator[F, Int].generate.map(diff => start + (diff % 1000)).flatMap { port =>
      MonadError[F, Throwable].handleErrorWith {
        Sync[F]
          .delay(new ServerSocket(port))
          .flatMap(serverSocket => Sync[F].delay(serverSocket.close()))
          .as(port)
      } { _ =>
        availablePort[F](start)
      }
    }

  def apiConfiguration[F[_]: Sync]: Resource[F, ApiConfiguration] =
    for {
      redisPort <- Resource.liftF(availablePort[F](6300))
      _ <- startEmbeddedRedis[F](redisPort)

      databaseName <- Resource.liftF(RandomGenerator[F, UUID].generate).map(_.toString)

      apiConfiguration = ApiConfiguration(
        h2DatabaseConfiguration(databaseName),
        DefaultHttpConfiguration,
        DefaultAuthenticationConfiguration,
        RedisConfiguration("localhost", redisPort, None),
        DefaultBuildInformation
      )

      _ <- Resource.liftF(MigrationApp.migrate(apiConfiguration.databaseConfiguration))
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
