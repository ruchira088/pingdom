package com.ruchij.core.kv.codec

import cats.effect.IO
import com.ruchij.core.kv.codec.KVDecoder.ItemLength._
import com.ruchij.core.kv.codec.KVDecoder._
import com.ruchij.core.test.utils.Providers.clock
import com.ruchij.core.types.JodaClock
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class KVDecoderSpec extends AnyFlatSpec with Matchers {

  "KVDecoder" should "decode String-String" in {
    "Hello".decode[IO, String].unsafeRunSync() mustBe "Hello"
  }

  it should "decode String-Numeric" in {
    "1".decode[IO, Long].unsafeRunSync() mustBe 1L
    "10000".decode[IO, Int].unsafeRunSync() mustBe 10_000
  }

  it should "decode String-DateTime" in {
    val dateTime = JodaClock[IO].currentTimestamp.map(_.withZone(DateTimeZone.UTC)).unsafeRunSync()

    dateTime.toString.decode[IO, DateTime].unsafeRunSync() mustBe dateTime
  }

  it should "decode String-(nested case class)" in {
    case class Adult(person: Person, job: String)
    case class Child(person: Person, father: Adult, mother: Adult)
    case class Person(name: String, age: Int)

    val father = Adult(Person("Harry", 40), "Engineer")
    val mother = Adult(Person("Mary", 38), "Scientist")

    val child = Child(Person("John", 5), father, mother)

    "John:::5:::Harry:::40:::Engineer:::Mary:::38:::Scientist".decode[IO, Child].unsafeRunSync() mustBe child
  }

}
