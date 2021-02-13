package com.ruchij

import cats.effect.{Async, Blocker, ContextShift, ExitCode, IO, IOApp, Sync}
import cats.implicits._
import com.ruchij.config.ServiceConfiguration
import com.ruchij.daos.account.DoobieAccountDao
import com.ruchij.daos.credentials.DoobieCredentialsDao
import com.ruchij.daos.doobie.DoobieTransactor
import com.ruchij.daos.user.DoobieUserDao
import com.ruchij.migration.MigrationApp
import com.ruchij.services.hash.{BCryptPasswordHashingService, PasswordHashingService}
import com.ruchij.services.health.{HealthService, HealthServiceImpl}
import com.ruchij.services.user.{UserService, UserServiceImpl}
import com.ruchij.types.CustomBlocker.{CpuBlocker, IOBlocker}
import com.ruchij.types.{FunctionKTypes, JodaClock}
import com.ruchij.web.Routes
import doobie.ConnectionIO
import org.http4s.HttpApp
import org.http4s.server.blaze.BlazeServerBuilder
import pureconfig.ConfigSource

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object App extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      configObjectSource <- IO.delay(ConfigSource.defaultApplication)
      serviceConfiguration <- ServiceConfiguration.parse[IO](configObjectSource)

      httpApp <- program[IO](serviceConfiguration)

      _ <- BlazeServerBuilder
        .apply[IO](ExecutionContext.global)
        .withHttpApp(httpApp)
        .bindHttp(serviceConfiguration.httpConfiguration.port, serviceConfiguration.httpConfiguration.host)
        .serve
        .compile
        .drain

    } yield ExitCode.Success

  def program[F[_]: Async: ContextShift: JodaClock](serviceConfiguration: ServiceConfiguration): F[HttpApp[F]] =
    for {
      ioThreadPool <- Sync[F].delay(Executors.newCachedThreadPool())
      ioBlocker = IOBlocker(Blocker.liftExecutionContext(ExecutionContext.fromExecutor(ioThreadPool)))

      cpuCount <- Sync[F].delay(Runtime.getRuntime.availableProcessors())
      cpuThreadPool <- Sync[F].delay(Executors.newFixedThreadPool(cpuCount))
      cpuBlocker = CpuBlocker(Blocker.liftExecutionContext(ExecutionContext.fromExecutor(cpuThreadPool)))

      httpApp <- program[F](serviceConfiguration, ioBlocker, cpuBlocker)
    } yield httpApp

  def program[F[_]: Async: ContextShift: JodaClock](
    serviceConfiguration: ServiceConfiguration,
    ioBlocker: IOBlocker,
    cpuBlocker: CpuBlocker
  ): F[HttpApp[F]] =
    MigrationApp
      .migrate[F](serviceConfiguration.databaseConfiguration)
      .productR {
        DoobieTransactor
          .create[F](serviceConfiguration.databaseConfiguration, ioBlocker)
          .map(transactor => FunctionKTypes.connectionIoToF[F](transactor))
          .map { implicit transactor =>
            val passwordHashingService: PasswordHashingService[F] = new BCryptPasswordHashingService[F](cpuBlocker)

            val healthService: HealthService[F] = new HealthServiceImpl[F](serviceConfiguration.buildInformation)
            val userService: UserService[F] =
              new UserServiceImpl[F, ConnectionIO](
                passwordHashingService,
                DoobieUserDao,
                DoobieAccountDao,
                DoobieCredentialsDao
              )

            Routes(userService, ???, healthService)
          }
      }

}
