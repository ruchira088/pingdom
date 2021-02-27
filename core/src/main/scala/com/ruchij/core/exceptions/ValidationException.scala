package com.ruchij.core.exceptions

import cats.data.NonEmptyList

case class ValidationException(errorMessages: NonEmptyList[String])
    extends Exception(errorMessages.toList.mkString("\n"))
