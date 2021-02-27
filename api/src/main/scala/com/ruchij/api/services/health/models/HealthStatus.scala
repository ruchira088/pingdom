package com.ruchij.api.services.health.models

import enumeratum.{Enum, EnumEntry}

sealed trait HealthStatus extends EnumEntry

object HealthStatus extends Enum[HealthStatus] {
  case object Healthy extends HealthStatus
  case object Unhealthy extends HealthStatus

  override def values: IndexedSeq[HealthStatus] = findValues
}
