package com.ruchij.kv

import dev.profunktor.redis4cats.RedisCommands

class RedisKeyValueStore[F[_], K, V](redisCommands: RedisCommands[F, String, String]) extends KeyValueStore[F, K, V] {
  override def put(key: K, value: V): F[Option[V]] = ???

  override def get(key: K): F[Option[V]] = ???

  override def delete(key: K): F[Option[V]] = ???
}
