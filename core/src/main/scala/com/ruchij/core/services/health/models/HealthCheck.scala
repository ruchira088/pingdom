package com.ruchij.core.services.health.models

import shapeless.Generic

case class HealthCheck(database: HealthStatus, keyValueStore: HealthStatus)

object HealthCheck {
  implicit class HealthCheckOps(healthCheck: HealthCheck) {
    val isHealthy: Boolean =
      Generic[HealthCheck].to(healthCheck).toList.forall(_ == HealthStatus.Healthy)
  }
}
