package com.ruchij.core.kv.codec

import cats.kernel.Monoid

trait Consolidator[A] extends Monoid[A] {

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

      override def split(value: String, count: Int): Option[(String, String)] =
        if (value == empty) None
        else {
          val terms = value.split(Delimiter)

          if (terms.length < count) None
          else Some(terms.take(count).mkString(Delimiter) -> terms.drop(count).mkString(Delimiter))
        }
    }

}
