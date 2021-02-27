package com.ruchij.core.syntax

import cats.data.NonEmptyList

object EitherOps {

  def combine[A](value: A)(rules: (Boolean, String)*): Either[NonEmptyList[String], A] =
    rules
      .collect { case (false, errorMessage) => errorMessage }
      .foldLeft[Either[NonEmptyList[String], A]](Right(value)) {
        case (Right(_), errorMessage) => Left(NonEmptyList.of(errorMessage))

        case (Left(list), errorMessage) => Left(list.append(errorMessage))
      }

}
