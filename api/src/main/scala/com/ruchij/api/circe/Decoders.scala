package com.ruchij.api.circe

import com.ruchij.core.daos.user.models.Email
import com.ruchij.api.services.user.models.Password
import com.ruchij.core.syntax.EitherOps
import io.circe.{Decoder, HCursor}
import org.http4s.{Header, Method}
import org.joda.time.DateTime

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Try

object Decoders {
  implicit val stringDecoder: Decoder[String] =
    Decoder.decodeString.emap { string =>
      if (string.nonEmpty) Right(string) else Left("must not be empty")
    }

  implicit val stringOptionDecoder: Decoder[Option[String]] =
    Decoder.decodeOption[String](Decoder.decodeString).map {
      case maybeString @ Some(value) if value.nonEmpty => maybeString

      case _ => None
    }

  implicit val dateTimeDecoder: Decoder[DateTime] =
    stringDecoder.emapTry(dateTimeString => Try(DateTime.parse(dateTimeString)))

  implicit val emailDecoder: Decoder[Email] = stringDecoder.emap(Email.from)

  implicit val finiteDuration: Decoder[FiniteDuration] =
    stringDecoder.emap {
      case Duration(length, unit) => Right(FiniteDuration(length, unit))

      case input => Left(s"""Unable to parse "$input" as a duration""")
    }

  implicit val headerDecoder: Decoder[Header] =
    (cursor: HCursor) =>
      for {
        name <- cursor.downField("name").as[String]
        value <- cursor.downField("value").as[String]
      } yield Header(name, value)

  implicit val methodDecoder: Decoder[Method] =
    stringDecoder.emap(value => Method.fromString(value).left.map(_.message))

  implicit val passwordDecoder: Decoder[Password] =
    stringDecoder.emap { string =>
      EitherOps
        .combine(Password(string))(
          (string.trim.length > 8) -> "must be longer than 8 characters",
          string.exists(_.isDigit) -> "must contain a digit",
          string.exists(_.isLetter) -> "must contain a letter"
        )
        .left
        .map(_.toList.mkString(", "))
    }

}
