package com.ruchij.api.kv

import com.ruchij.core.kv.codec.Keyspace
import org.joda.time.DateTime

case object HealthCheckKeyspace extends Keyspace[String, DateTime] {
  override val name: String = "health-check"
}
