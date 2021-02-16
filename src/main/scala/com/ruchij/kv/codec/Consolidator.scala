package com.ruchij.kv.codec

import cats.kernel.Monoid

trait Consolidator[A] extends Monoid[A] {
  def split(value: A): Option[(A, A)]

  def split(value: A, count: Int): Option[(A, A)]
}

object Consolidator {
  def apply[A](implicit consolidator: Consolidator[A]): Consolidator[A] = consolidator

  implicit val stringConsolidator: Consolidator[String] =
    new Consolidator[String] {
      val Delimiter: String = ":::"

      override def empty: String = ""

      override def combine(x: String, y: String): String =
        if (x.isEmpty) y else if (y.isEmpty) x else x + Delimiter + y

      override def split(value: String): Option[(String, String)] = {
        val terms = value.split(Delimiter).filter(_.nonEmpty)

        terms.headOption.map { _ -> terms.tail.mkString(Delimiter) }
      }

      override def split(value: String, count: Int): Option[(String, String)] =
        count match {
          case 0 => None

          case 1 => split(value)

          case n =>
            split(value)
              .flatMap {
                case (first, rest) =>
                  split(rest, n - 1).map { case (second, tail) => (first + Delimiter + second) -> tail }
              }
        }
    }
}