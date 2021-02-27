package com.ruchij.api.services.health

import cats.effect.{Concurrent, Timer}
import cats.implicits._
import cats.~>
import com.ruchij.api.services.health.models.{HealthCheck, HealthStatus, ServiceInformation}
import com.ruchij.core.config.BuildInformation
import com.ruchij.core.kv.KeyValueStore
import com.ruchij.core.types.{JodaClock, RandomGenerator}
import doobie.ConnectionIO
import doobie.implicits._
import org.joda.time.DateTime

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class HealthServiceImpl[F[_]: Timer: Concurrent](
  keyValueStore: KeyValueStore[F, String, DateTime],
  buildInformation: BuildInformation
)(implicit transaction: ConnectionIO ~> F)
    extends HealthService[F] {

  val databaseHealthCheck: F[HealthStatus] =
    transaction(sql"SELECT 1".query[Int].unique)
      .map(_ == 1)
      .as(HealthStatus.Healthy)

  val keyValueStoreHealthCheck: F[HealthStatus] =
    for {
      key <- RandomGenerator[F, UUID].generate.map(_.toString)
      value <- JodaClock[F].currentTimestamp

      _ <- keyValueStore.put(key, value)
      retrievedValue <- keyValueStore.get(key)
      deletedValue <- keyValueStore.delete(key)

      isSuccess = retrievedValue.map(_.getMillis).contains(value.getMillis) &&
        deletedValue.map(_.getMillis).contains(value.getMillis)

      healthStatus = if (isSuccess) HealthStatus.Healthy else HealthStatus.Unhealthy
    } yield healthStatus

  override val serviceInformation: F[ServiceInformation] =
    JodaClock[F].currentTimestamp
      .flatMap(timestamp => ServiceInformation.create[F](timestamp, buildInformation))

  override val healthCheck: F[HealthCheck] =
    for {
      databaseFiber <- Concurrent[F].start(timeout(databaseHealthCheck))
      keyValueStoreFiber <- Concurrent[F].start(keyValueStoreHealthCheck)

      database <- databaseFiber.join
      keyValueStore <- keyValueStoreFiber.join
    } yield HealthCheck(database, keyValueStore)

  def timeout(value: F[HealthStatus]): F[HealthStatus] =
    Concurrent[F]
      .race(value, Timer[F].sleep(FiniteDuration(10, TimeUnit.SECONDS)).as(HealthStatus.Unhealthy))
      .map(_.merge)

}
