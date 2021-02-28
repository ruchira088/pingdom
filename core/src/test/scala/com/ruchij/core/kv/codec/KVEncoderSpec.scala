package com.ruchij.core.kv.codec

import cats.effect.IO
import com.ruchij.core.kv.codec.KVEncoder._
import com.ruchij.core.test.utils.Providers._
import com.ruchij.core.types.JodaClock
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class KVEncoderSpec extends AnyFlatSpec with Matchers {

  "KVEncoder" should "encode String-String values" in {
    "Hello".encode[IO, String].unsafeRunSync() mustBe "Hello"
    "Hello World".encode[IO, String].unsafeRunSync() mustBe "Hello World"
  }

  it should "encode Numeric-String" in {
    1L.encode[IO, String].unsafeRunSync() mustBe "1"
    1.23.encode[IO, String].unsafeRunSync() mustBe "1.23"
    10_000.encode[IO, String].unsafeRunSync() mustBe "10000"
  }

  it should "encode DateTime-String values" in {
    val dateTime: DateTime = JodaClock[IO].currentTimestamp.map(_.withZone(DateTimeZone.UTC)).unsafeRunSync()

    dateTime.encode[IO, String].unsafeRunSync() mustBe dateTime.toString
  }

  it should "encode (nested case class)-String values" in {
    case class Adult(person: Person, job: String)
    case class Child(person: Person, father: Adult, mother: Adult)
    case class Person(name: String, age: Int)

    val father = Adult(Person("Harry", 40), "Engineer")
    val mother = Adult(Person("Mary", 38), "Scientist")

    val child = Child(Person("John", 5), father, mother)

    child.encode[IO, String].unsafeRunSync() mustBe "John:::5:::Harry:::40:::Engineer:::Mary:::38:::Scientist"
  }

}
