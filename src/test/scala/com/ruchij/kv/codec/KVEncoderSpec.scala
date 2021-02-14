package com.ruchij.kv.codec

import cats.effect.IO
import com.ruchij.daos.auth.models.AuthenticationToken
import com.ruchij.kv.codec.KVEncoder._
import com.ruchij.test.utils.Providers._
import com.ruchij.types.JodaClock
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class KVEncoderSpec extends AnyFlatSpec with Matchers {

  "KVEncoder" should "encode String values correctly" in {
    "Hello".encode[IO, String].unsafeRunSync() mustBe "Hello"
    "Hello World".encode[IO, String].unsafeRunSync() mustBe "Hello World"
  }

  it should "encode numeric values correctly" in {
    1.encode[IO, String].unsafeRunSync() mustBe "1"
    1.23.encode[IO, String].unsafeRunSync() mustBe "1.23"
    10_000.encode[IO, String].unsafeRunSync() mustBe "10000"
  }

  it should "encode DateTime values correctly" in {
    val dateTime = JodaClock[IO].currentTimestamp.unsafeRunSync()

    dateTime.encode[IO, String].unsafeRunSync() mustBe dateTime.getMillis.toString
  }

  it should "encode AuthenticationToken values correctly" in {
    val dateTime = JodaClock[IO].currentTimestamp.unsafeRunSync()

    val authenticationToken = AuthenticationToken(dateTime, dateTime, "my-user-id", "my-secret", 1)

    authenticationToken.encode[IO, String].unsafeRunSync() mustBe
      s"${dateTime.getMillis}:::${dateTime.getMillis}:::my-user-id:::my-secret:::1"
  }

}
