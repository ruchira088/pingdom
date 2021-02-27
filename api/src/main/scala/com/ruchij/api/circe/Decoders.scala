package com.ruchij.api.circe

import com.ruchij.core.daos.user.models.Email
import com.ruchij.core.services.user.models.Password
import com.ruchij.core.syntax.EitherOps
import io.circe.Decoder
import org.joda.time.DateTime

import scala.util.Try

object Decoders {

  implicit val stringDecoder: Decoder[String] =
    Decoder.decodeString.emap { string =>
      if (string.nonEmpty) Right(string) else Left("must not be empty")
    }

  implicit val stringOptionDecoder: Decoder[Option[String]] =
    Decoder.decodeString.map { string =>
      if (string.nonEmpty) Some(string) else None
    }

  implicit val dateTimeDecoder: Decoder[DateTime] =
    stringDecoder.emapTry(dateTimeString => Try(DateTime.parse(dateTimeString)))

  implicit val emailDecoder: Decoder[Email] = stringDecoder.emap(Email.from)

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
