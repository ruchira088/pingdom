package com.ruchij.kv

import com.ruchij.kv.codec.{Consolidator, Keyspace}

class KeyspacedKeyValueStore[F[_], K: Consolidator, V](keyValueStore: KeyValueStore[F, K, V], keyspace: Keyspace[K, V])
    extends KeyValueStore[F, K, V] {

  override def put(key: K, value: V): F[Option[V]] =
    keyValueStore.put(keyspacedKey(key), value)

  override def get(key: K): F[Option[V]] =
    keyValueStore.get(keyspacedKey(key))

  override def delete(key: K): F[Option[V]] =
    keyValueStore.delete(keyspacedKey(key))

  def keyspacedKey(key: K): K =
    Consolidator[K].combine(keyspace.name, key)

}
