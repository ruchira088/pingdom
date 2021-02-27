package com.ruchij.kv.codec

import com.ruchij.daos.auth.models.AuthenticationToken
import org.joda.time.DateTime

trait Keyspace[K, V] {
  val name: K
}

object Keyspace {
  case object AuthenticationKeyspace extends Keyspace[String, AuthenticationToken] {
    override val name: String = "authentication"
  }

  case object HealthCheckKeyspace extends Keyspace[String, DateTime] {
    override val name: String = "health-check"
  }
}
