package com.ruchij.types

import cats.effect.Sync

import java.util.UUID
import scala.util.Random

trait RandomGenerator[F[_], +A] {

  def generate[B >: A]: F[B]

}

object RandomGenerator {

  def apply[F[_], A](implicit randomGenerator: RandomGenerator[F, A]): RandomGenerator[F, A] = randomGenerator

  def from[F[_]: Sync, A](eval: => A): RandomGenerator[F, A] =
    new RandomGenerator[F, A] {
      override def generate[B >: A]: F[B] = Sync[F].delay(eval)
    }

  implicit def uuidGenerator[F[_]: Sync]: RandomGenerator[F, UUID] = from[F, UUID](UUID.randomUUID())

  implicit def intGenerator[F[_]: Sync]: RandomGenerator[F, Int] = from[F, Int](Random.nextInt())

}