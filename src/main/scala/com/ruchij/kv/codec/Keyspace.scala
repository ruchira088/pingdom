package com.ruchij.kv.codec

import com.ruchij.daos.auth.models.AuthenticationToken

trait Keyspace[K, V] {
  val name: K
}

object Keyspace {
  case object AuthenticationKeyspace extends Keyspace[String, AuthenticationToken] {
    override val name: String = "authentication"
  }
}
