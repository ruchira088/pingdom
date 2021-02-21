package com.ruchij

import cats.effect.{Blocker, Concurrent, ContextShift, ExitCode, IO, IOApp, Resource, Sync, Timer}
import cats.implicits._
import com.ruchij.config.ServiceConfiguration
import com.ruchij.daos.account.DoobieAccountDao
import com.ruchij.daos.auth.AuthenticationTokenKeyValueStore
import com.ruchij.daos.auth.models.AuthenticationToken
import com.ruchij.daos.credentials.DoobieCredentialsDao
import com.ruchij.daos.doobie.DoobieTransactor
import com.ruchij.daos.user.DoobieUserDao
import com.ruchij.kv.codec.Keyspace
import com.ruchij.kv.{KeyspacedKeyValueStore, RedisKeyValueStore}
import com.ruchij.migration.MigrationApp
import com.ruchij.services.auth.AuthenticationServiceImpl
import com.ruchij.services.hash.{BCryptPasswordHashingService, PasswordHashingService}
import com.ruchij.services.health.{HealthService, HealthServiceImpl}
import com.ruchij.services.user.{UserService, UserServiceImpl}
import com.ruchij.types.CustomBlocker.{CpuBlocker, IOBlocker}
import com.ruchij.types.FunctionKTypes
import com.ruchij.web.Routes
import dev.profunktor.redis4cats.effect.Log.Stdout.instance
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import doobie.ConnectionIO
import org.http4s.HttpApp
import org.http4s.server.blaze.BlazeServerBuilder
import org.joda.time.DateTime
import pureconfig.ConfigSource

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object App extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      configObjectSource <- IO.delay(ConfigSource.defaultApplication)
      serviceConfiguration <- ServiceConfiguration.parse[IO](configObjectSource)

      _ <-
        program[IO](serviceConfiguration).use {
          httpApp =>
            BlazeServerBuilder
              .apply[IO](ExecutionContext.global)
              .withHttpApp(httpApp)
              .bindHttp(serviceConfiguration.httpConfiguration.port, serviceConfiguration.httpConfiguration.host)
              .serve
              .compile
              .drain
        }
    } yield ExitCode.Success

  def program[F[_]: ContextShift: Timer: Concurrent](serviceConfiguration: ServiceConfiguration): Resource[F, HttpApp[F]] =
    for {
      ioThreadPool <-
        Resource.make(Sync[F].delay(Executors.newCachedThreadPool())) {
          executorService => Sync[F].delay(executorService.shutdown())
        }
      ioBlocker = IOBlocker(Blocker.liftExecutionContext(ExecutionContext.fromExecutor(ioThreadPool)))

      cpuCount <- Resource.liftF(Sync[F].delay(Runtime.getRuntime.availableProcessors()))
      cpuThreadPool <- Resource.make(Sync[F].delay(Executors.newFixedThreadPool(cpuCount))) {
        executorService => Sync[F].delay(executorService.shutdown())
      }
      cpuBlocker = CpuBlocker(Blocker.liftExecutionContext(ExecutionContext.fromExecutor(cpuThreadPool)))

      redisCommands <- Redis[F].utf8(serviceConfiguration.redisConfiguration.url)

      httpApp <- Resource.liftF {
        program(ioBlocker, cpuBlocker, redisCommands, serviceConfiguration)
      }
    }
    yield httpApp

  def program[F[_]: Concurrent: ContextShift: Timer](
    ioBlocker: IOBlocker,
    cpuBlocker: CpuBlocker,
    redisCommands: RedisCommands[F, String, String],
    serviceConfiguration: ServiceConfiguration
  ): F[HttpApp[F]] =
    MigrationApp
      .migrate[F](serviceConfiguration.databaseConfiguration)
      .productR {
        DoobieTransactor
          .create[F](serviceConfiguration.databaseConfiguration, ioBlocker)
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
              new HealthServiceImpl[F](healthCheckKeyValueStore, serviceConfiguration.buildInformation)

            val userService: UserService[F] =
              new UserServiceImpl[F, ConnectionIO](
                passwordHashingService,
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
                serviceConfiguration.authenticationConfiguration
              )

            Routes(userService, authenticationService, healthService)
          }
      }

}
