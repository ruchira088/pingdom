package com.ruchij.kv.codec

import cats.effect.IO
import com.ruchij.daos.auth.models.AuthenticationToken
import com.ruchij.kv.codec.KVDecoder._
import com.ruchij.test.utils.Providers.clock
import com.ruchij.types.JodaClock
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

  it should "decode String-AuthenticationToken" in {
    val dateTime: DateTime = JodaClock[IO].currentTimestamp.map(_.withZone(DateTimeZone.UTC)).unsafeRunSync()

    val input = s"$dateTime:::$dateTime:::my-user-id:::my-secret:::1"

    val authenticationToken: AuthenticationToken =
      AuthenticationToken(dateTime, dateTime, "my-user-id", "my-secret", 1)

    input.decode[IO, AuthenticationToken].unsafeRunSync() mustBe authenticationToken
  }

  it should "decode String-(nested case class)" in {
    case class Adult(name: String, age: Int)
    case class Child(name: String, age: Int, father: Adult, mother: Adult)

    val father = Adult("Harry", 40)
    val mother = Adult("Mary", 38)

    val child = Child("John",5, father, mother)

    "John:::5:::Harry:::40:::Mary:::38".decode[IO, Child].unsafeRunSync() mustBe child
  }

}
