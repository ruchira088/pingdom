package com.ruchij.core.services.health

import com.ruchij.core.services.health.models.{HealthCheck, ServiceInformation}

trait HealthService[F[_]] {

  val serviceInformation: F[ServiceInformation]

  val healthCheck: F[HealthCheck]

}
