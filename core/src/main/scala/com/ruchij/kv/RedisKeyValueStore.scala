package com.ruchij.kv

import cats.Monad
import cats.implicits._
import com.ruchij.kv.codec.{KVCodec, KVDecoder, KVEncoder}
import dev.profunktor.redis4cats.RedisCommands

class RedisKeyValueStore[F[_]: Monad, K: KVEncoder[F, *, String], V: KVCodec[F, *, String]](
  redisCommands: RedisCommands[F, String, String]
) extends KeyValueStore[F, K, V] {

  override def put(key: K, value: V): F[Option[V]] =
    for {
      maybeExistingValue <- get(key)

      encodedKey <- KVEncoder[F, K, String].encode(key)
      encodedValue <- KVEncoder[F, V, String].encode(value)

      _ <- redisCommands.set(encodedKey, encodedValue)
    }
    yield maybeExistingValue

  override def get(key: K): F[Option[V]] =
    for {
      encodedKey <- KVEncoder[F, K, String].encode(key)
      maybeEncodedValue <- redisCommands.get(encodedKey)
      maybeValue <- maybeEncodedValue.traverse(value => KVDecoder[F, String, V].decode(value))
    }
    yield maybeValue

  override def delete(key: K): F[Option[V]] =
    for {
      maybeValue <- get(key)

      encodedKey <- KVEncoder[F, K, String].encode(key)
      _ <- redisCommands.del(encodedKey)
    }
    yield maybeValue

}
