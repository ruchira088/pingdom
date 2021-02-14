package com.ruchij.kv.codec

import cats.kernel.Monoid

trait Consolidator[A] extends Monoid[A] {
  def split(value: A): Option[(A, A)]
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
    }
}