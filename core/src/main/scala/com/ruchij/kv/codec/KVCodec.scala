package com.ruchij.kv.codec

trait KVCodec[F[_], A, B] extends KVDecoder[F, B, A] with KVEncoder[F, A, B]

object KVCodec {

  def apply[F[_], A, B](implicit kvCodec: KVCodec[F, A, B]): KVCodec[F, A, B] = kvCodec

  implicit def from[F[_], A, B](
    implicit kvDecoder: KVDecoder[F, B, A],
    kvEncoder: KVEncoder[F, A, B]
  ): KVCodec[F, A, B] =
    new KVCodec[F, A, B] {
      override def encode[C >: B](value: A): F[C] = kvEncoder.encode(value)

      override def decode(value: B): F[A] = kvDecoder.decode(value)
    }

}
