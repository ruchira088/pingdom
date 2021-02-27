package com.ruchij.core.kv

trait KeyValueStore[F[_], K, V] {

  def put(key: K, value: V): F[Option[V]]

  def get(key: K): F[Option[V]]

  def delete(key: K): F[Option[V]]

}
