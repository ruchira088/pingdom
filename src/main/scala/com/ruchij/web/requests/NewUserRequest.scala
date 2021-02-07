package com.ruchij.web.requests

import cats.data.NonEmptyList
import com.ruchij.daos.user.models.Email
import com.ruchij.services.user.models.Password
import com.ruchij.web.validator.Validator

case class NewUserRequest(firstName: String, lastName: String, email: Email, password: Password)

object NewUserRequest {
  implicit val newUserRequestValidator: Validator[NewUserRequest] = {
    new Validator[NewUserRequest] {
      override def validate[B <: NewUserRequest](newUserRequest: B): Either[NonEmptyList[String], B] =
        Validator.combine(newUserRequest)(
          newUserRequest.firstName.trim.nonEmpty -> "firstName cannot be empty",
          newUserRequest.lastName.trim.nonEmpty -> "lastName cannot be empty"
        )
    }
  }
}
