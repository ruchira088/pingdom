package com.ruchij.core.kv.codec

trait Keyspace[K, V] {
  val name: K
}
