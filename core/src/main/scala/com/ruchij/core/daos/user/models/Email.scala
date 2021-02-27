package com.ruchij.core.daos.user.models

import scala.util.matching.Regex

case class Email(value: String) extends AnyVal

object Email {
  val EmailRegex: Regex = "\\S+@\\S+\\.\\S+".r

  def from(input: String): Either[String, Email] =
    if (EmailRegex.matches(input)) Right(Email(input)) else Left(s"""Unable to parse "$input" as an email address""")
}
