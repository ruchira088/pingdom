package com.ruchij.test.utils

import cats.data.NonEmptyList
import com.ruchij.test.utils.JsonUtils.JsonMismatchError
import io.circe.Json
import io.circe.literal.JsonStringContext
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class JsonUtilsSpec extends AnyFlatSpec with Matchers {

  "contains(Json, Json): Boolean" should "return if the partial JSON is contained within the full JSON" in {
    JsonUtils.contains(
      json"""{ "name": "John", "age": 10 }""",
      json"""{ "name": "John" }"""
    ) mustBe Right((): Unit)

    JsonUtils.contains(
      json"""{ "name": "Adam", "age": 10 }""",
      json"""{ "name": "John" }"""
    ) mustBe Left {
      NonEmptyList.of {
        JsonMismatchError(List("name"), Json.fromString("John"), Some(Json.fromString("Adam")))
      }
    }

    JsonUtils.contains(
      json"""{ "person": { "name": "Adam", "age": 10 } }""",
      json"""{ "person": { "age": 10 } }"""
    ) mustBe Right((): Unit)

    JsonUtils.contains(
      json"""{ "person": { "name": { "first": "John", "last": "Adams" }, "age": 10 } }""",
      json"""{ "person": { "name": { "first": "John", "last": "Smith" }, "age": 20 } }"""
    ) mustBe Left {
      NonEmptyList.of(
        JsonMismatchError(List("person", "name", "last"), Json.fromString("Smith"), Some(Json.fromString("Adams"))),
        JsonMismatchError(List("person", "age"), Json.fromInt(20), Some(Json.fromInt(10)))
      )
    }

  }
}
