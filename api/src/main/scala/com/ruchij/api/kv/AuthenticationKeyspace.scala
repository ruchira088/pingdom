package com.ruchij.api.kv

import com.ruchij.api.daos.auth.models.AuthenticationToken
import com.ruchij.core.kv.codec.Keyspace

case object AuthenticationKeyspace extends Keyspace[String, AuthenticationToken] {
  override val name: String = "authentication"
}
