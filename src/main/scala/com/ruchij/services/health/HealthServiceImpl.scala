package com.ruchij.services.health

import cats.effect.Sync
import cats.implicits._
import com.ruchij.config.BuildInformation
import com.ruchij.services.health.models.ServiceInformation
import com.ruchij.types.JodaClock

class HealthServiceImpl[F[_]: JodaClock: Sync](buildInformation: BuildInformation) extends HealthService[F] {
  override def serviceInformation(): F[ServiceInformation] =
    JodaClock[F].currentTimestamp
      .flatMap(timestamp => ServiceInformation.create(timestamp, buildInformation))
}

