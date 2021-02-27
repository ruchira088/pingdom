package com.ruchij.api

import cats.effect.{Blocker, Concurrent, ContextShift, ExitCode, IO, IOApp, Resource, Sync, Timer}
import cats.implicits._
import com.ruchij.api.config.ApiConfiguration
import com.ruchij.core.daos.account.DoobieAccountDao
import com.ruchij.core.daos.auth.AuthenticationTokenKeyValueStore
import com.ruchij.core.daos.auth.models.AuthenticationToken
import com.ruchij.core.daos.credentials.DoobieCredentialsDao
import com.ruchij.core.daos.doobie.DoobieTransactor
import com.ruchij.core.daos.permission.DoobiePermissionDao
import com.ruchij.core.daos.user.DoobieUserDao
import com.ruchij.core.kv.codec.Keyspace
import com.ruchij.core.kv.{KeyspacedKeyValueStore, RedisKeyValueStore}
import com.ruchij.migration.MigrationApp
import com.ruchij.core.services.auth.AuthenticationServiceImpl
import com.ruchij.core.services.hash.{BCryptPasswordHashingService, PasswordHashingService}
import com.ruchij.core.services.health.{HealthService, HealthServiceImpl}
import com.ruchij.core.services.user.{UserService, UserServiceImpl}
import com.ruchij.core.types.CustomBlocker.{CpuBlocker, IOBlocker}
import com.ruchij.core.types.FunctionKTypes
import com.ruchij.api.web.Routes
import dev.profunktor.redis4cats.effect.Log.Stdout.instance
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import doobie.ConnectionIO
import org.http4s.HttpApp
import org.http4s.server.blaze.BlazeServerBuilder
import org.joda.time.DateTime
import pureconfig.ConfigSource

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object ApiApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      configObjectSource <- IO.delay(ConfigSource.defaultApplication)
      apiConfiguration <- ApiConfiguration.parse[IO](configObjectSource)

      _ <- program[IO](apiConfiguration).use { httpApp =>
        BlazeServerBuilder
          .apply[IO](ExecutionContext.global)
          .withHttpApp(httpApp)
          .bindHttp(apiConfiguration.httpConfiguration.port, apiConfiguration.httpConfiguration.host)
          .serve
          .compile
          .drain
      }
    } yield ExitCode.Success

  def program[F[_]: ContextShift: Timer: Concurrent](apiConfiguration: ApiConfiguration): Resource[F, HttpApp[F]] =
    for {
      ioThreadPool <- Resource.make(Sync[F].delay(Executors.newCachedThreadPool())) { executorService =>
        Sync[F].delay(executorService.shutdown())
      }
      ioBlocker = IOBlocker(Blocker.liftExecutionContext(ExecutionContext.fromExecutor(ioThreadPool)))

      cpuCount <- Resource.liftF(Sync[F].delay(Runtime.getRuntime.availableProcessors()))
      cpuThreadPool <- Resource.make(Sync[F].delay(Executors.newFixedThreadPool(cpuCount))) { executorService =>
        Sync[F].delay(executorService.shutdown())
      }
      cpuBlocker = CpuBlocker(Blocker.liftExecutionContext(ExecutionContext.fromExecutor(cpuThreadPool)))

      redisCommands <- Redis[F].utf8(apiConfiguration.redisConfiguration.url)

      httpApp <- Resource.liftF {
        program(ioBlocker, cpuBlocker, redisCommands, apiConfiguration)
      }
    } yield httpApp

  def program[F[_]: Concurrent: ContextShift: Timer](
    ioBlocker: IOBlocker,
    cpuBlocker: CpuBlocker,
    redisCommands: RedisCommands[F, String, String],
    apiConfiguration: ApiConfiguration
  ): F[HttpApp[F]] =
    MigrationApp
      .migrate[F](apiConfiguration.databaseConfiguration)
      .productR {
        DoobieTransactor
          .create[F](apiConfiguration.databaseConfiguration, ioBlocker)
          .map(transactor => FunctionKTypes.connectionIoToF[F](transactor))
          .map { implicit transactor =>
            val passwordHashingService: PasswordHashingService[F] = new BCryptPasswordHashingService[F](cpuBlocker)

            val authenticationTokenStore: KeyspacedKeyValueStore[F, String, AuthenticationToken] =
              new KeyspacedKeyValueStore[F, String, AuthenticationToken](
                new RedisKeyValueStore(redisCommands),
                Keyspace.AuthenticationKeyspace
              )

            val healthCheckKeyValueStore =
              new KeyspacedKeyValueStore[F, String, DateTime](
                new RedisKeyValueStore(redisCommands),
                Keyspace.HealthCheckKeyspace
              )

            val authenticationTokenDao: AuthenticationTokenKeyValueStore[F] =
              new AuthenticationTokenKeyValueStore[F](authenticationTokenStore)

            val healthService: HealthService[F] =
              new HealthServiceImpl[F](healthCheckKeyValueStore, apiConfiguration.buildInformation)

            val userService: UserService[F] =
              new UserServiceImpl[F, ConnectionIO](
                passwordHashingService,
                DoobieUserDao,
                DoobieAccountDao,
                DoobieCredentialsDao,
                DoobiePermissionDao
              )

            val authenticationService: AuthenticationServiceImpl[F, ConnectionIO] =
              new AuthenticationServiceImpl[F, ConnectionIO](
                passwordHashingService,
                DoobieUserDao,
                DoobieCredentialsDao,
                authenticationTokenDao,
                apiConfiguration.authenticationConfiguration
              )

            Routes(userService, authenticationService, healthService)
          }
      }

}
