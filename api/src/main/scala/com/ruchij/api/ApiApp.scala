package com.ruchij.api

import cats.effect.{Blocker, Concurrent, ContextShift, ExitCode, IO, IOApp, Resource, Sync, Timer}
import cats.implicits._
import com.ruchij.api.config.ApiConfiguration
import com.ruchij.api.daos.authentication.AuthenticationTokenKeyValueStore
import com.ruchij.api.daos.authentication.models.AuthenticationToken
import com.ruchij.api.daos.credentials.DoobieCredentialsDao
import com.ruchij.api.kv.{AuthenticationKeyspace, HealthCheckKeyspace}
import com.ruchij.api.services.auth.AuthenticationServiceImpl
import com.ruchij.api.services.health.{HealthService, HealthServiceImpl}
import com.ruchij.api.services.user.{UserService, UserServiceImpl}
import com.ruchij.api.web.Routes
import com.ruchij.core.daos.account.DoobieAccountDao
import com.ruchij.core.daos.doobie.DoobieTransactor
import com.ruchij.core.daos.permission.DoobiePermissionDao
import com.ruchij.core.daos.ping.DoobiePingDao
import com.ruchij.core.daos.user.DoobieUserDao
import com.ruchij.core.kv.{KeyspacedKeyValueStore, RedisKeyValueStore}
import com.ruchij.core.services.authorization.AuthorizationServiceImpl
import com.ruchij.core.services.hash.{BCryptPasswordHashingService, PasswordHashingService}
import com.ruchij.core.services.ping.{PingService, PingServiceImpl}
import com.ruchij.core.types.CustomBlocker.{CpuBlocker, IOBlocker}
import com.ruchij.core.types.FunctionKTypes
import com.ruchij.migration.MigrationApp
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

      cpuCount <- Resource.eval(Sync[F].delay(Runtime.getRuntime.availableProcessors()))
      cpuThreadPool <- Resource.make(Sync[F].delay(Executors.newFixedThreadPool(cpuCount))) { executorService =>
        Sync[F].delay(executorService.shutdown())
      }
      cpuBlocker = CpuBlocker(Blocker.liftExecutionContext(ExecutionContext.fromExecutor(cpuThreadPool)))

      redisCommands <- Redis[F].utf8(apiConfiguration.redisConfiguration.url)

      httpApp <- Resource.eval {
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
                AuthenticationKeyspace
              )

            val healthCheckKeyValueStore =
              new KeyspacedKeyValueStore[F, String, DateTime](
                new RedisKeyValueStore(redisCommands),
                HealthCheckKeyspace
              )

            val authenticationTokenDao: AuthenticationTokenKeyValueStore[F] =
              new AuthenticationTokenKeyValueStore[F](authenticationTokenStore)

            val healthService: HealthService[F] =
              new HealthServiceImpl[F](healthCheckKeyValueStore, apiConfiguration.buildInformation)

            val authorizationService: AuthorizationServiceImpl[F, ConnectionIO] =
              new AuthorizationServiceImpl[F, ConnectionIO](DoobiePermissionDao)

            val userService: UserService[F] =
              new UserServiceImpl[F, ConnectionIO](
                passwordHashingService,
                authorizationService,
                DoobieUserDao,
                DoobieAccountDao,
                DoobieCredentialsDao
              )

            val authenticationService: AuthenticationServiceImpl[F, ConnectionIO] =
              new AuthenticationServiceImpl[F, ConnectionIO](
                passwordHashingService,
                DoobieUserDao,
                DoobieCredentialsDao,
                authenticationTokenDao,
                apiConfiguration.authenticationConfiguration
              )

            val pingService: PingService[F] =
              new PingServiceImpl[F, ConnectionIO](authorizationService, DoobiePingDao)

            Routes(userService, pingService, authenticationService, healthService)
          }
      }

}
