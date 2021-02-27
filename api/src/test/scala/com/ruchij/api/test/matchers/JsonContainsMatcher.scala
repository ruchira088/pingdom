package com.ruchij.api.test.matchers

import cats.effect.{Effect, Sync}
import cats.implicits._
import com.ruchij.api.test.utils.JsonUtils
import io.circe.Json
import org.http4s.Response
import org.scalatest.matchers.{MatchResult, Matcher}

class JsonContainsMatcher[F[_]: Sync: Effect](json: Json) extends Matcher[Response[F]] {

  override def apply(response: Response[F]): MatchResult =
    Effect[F]
      .toIO {
        JsonUtils.fromResponse(response).map { responseJson =>
          JsonUtils
            .contains(responseJson, json)
            .fold(
              errors => MatchResult(matches = false, errors.toList.mkString("\n"), "N/A"),
              _ => MatchResult(matches = true, "N/A", "Success")
            )

        }
      }
      .unsafeRunSync()

}
