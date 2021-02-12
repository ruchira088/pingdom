package com.ruchij.circe

import com.ruchij.daos.user.models.Email
import com.ruchij.services.user.models.Password
import com.ruchij.syntax.EitherOps
import io.circe.Decoder
import org.joda.time.DateTime

import scala.util.Try

object Decoders {
  implicit val dateTimeDecoder: Decoder[DateTime] =
    Decoder.decodeString.emapTry(dateTimeString => Try(DateTime.parse(dateTimeString)))

  implicit val emailDecoder: Decoder[Email] = Decoder.decodeString.emap(Email.from)

  implicit val passwordDecoder: Decoder[Password] =
    Decoder.decodeString.emap { string =>
      EitherOps.combine(Password(string))(
        string.trim.nonEmpty -> "must not be empty",
        (string.trim.length > 8) -> "must be longer than 8 characters",
        string.exists(_.isDigit) -> "must contain a digit",
        string.exists(_.isLetter) -> "must contain a letter"
      )
        .left
        .map(_.toList.mkString(", "))
    }
}
