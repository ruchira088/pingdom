package com.ruchij.api.test.utils

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import io.circe.Json
import io.circe.parser.{parse => parseJson}
import com.ruchij.core.types.FunctionKTypes.eitherToF
import org.http4s.Response

object JsonUtils {
  case class JsonMismatchError(path: List[String], expected: Json, actual: Option[Json]) extends Exception {
    override def getMessage: String =
      s"Expected: ${expected.spaces2} at path: ${path.mkString(",")}, but found: ${actual.map(_.spaces2).getOrElse("noting")}"
  }

  def fromResponse[F[_]: Sync](response: Response[F]): F[Json] =
    response.bodyText
      .compile[F, F, String]
      .string
      .flatMap { text =>
        eitherToF[Throwable, F].apply[Json](parseJson(text))
      }

  def contains(full: Json, part: Json): Either[NonEmptyList[JsonMismatchError], Unit] =
    full.asObject
      .flatMap { fullJson =>
        part.asObject.map(fullJson -> _)
      }
      .map {
        case (fullJson, partJson) =>
          partJson.toList.map {
            case (key, value) =>
              fullJson(key)
                .fold[Either[NonEmptyList[JsonMismatchError], Unit]](
                  Left(NonEmptyList.of(JsonMismatchError(List(key), value, None)))
                ) { jsonValue =>
                  contains(jsonValue, value).left.map {
                    _.map(jsonMissing => jsonMissing.copy(path = key :: jsonMissing.path))
                  }
                }
          }
      }
      .fold(
        if (full == part) Right((): Unit)
        else Left(NonEmptyList.of(JsonMismatchError(List.empty, part, Some(full))))
      ) {
        _.collect { case Left(value) => value }
          .foldLeft[Either[NonEmptyList[JsonMismatchError], Unit]](Right((): Unit)) {
            case (Left(accErrors), errors) => Left(accErrors ::: errors)

            case (_, errors) => Left(errors)
          }
      }

}
