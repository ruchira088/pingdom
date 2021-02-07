package com.ruchij.web.validator

import cats.SemigroupK
import cats.data.NonEmptyList
import cats.effect.MonadThrow
import cats.implicits._
import com.ruchij.exceptions.ValidationException
import com.ruchij.types.FunctionKTypes
import org.http4s.{EntityDecoder, Request}

trait Validator[-A] {
  def validate[B <: A](input: B): Either[NonEmptyList[String], B]
}

object Validator {
  implicit val validatorSemigroupK: SemigroupK[Validator] =
    new SemigroupK[Validator] {
      override def combineK[A](x: Validator[A], y: Validator[A]): Validator[A] =
        new Validator[A] {
          override def validate[B <: A](input: B): Either[NonEmptyList[String], B] = {
            x.validate(input).fold(
              errors => Left(y.validate(input).fold(otherErrors => errors ::: otherErrors, _ => errors)),
              _ => y.validate(input)
            )
          }
        }
    }

  def apply[A](implicit validator: Validator[A]): Validator[A] = validator

  def combine[A](value: A)(rules: (Boolean, String)*): Either[NonEmptyList[String], A] =
    rules
      .collect { case (false, errorMessage) => errorMessage }
      .foldLeft[Either[NonEmptyList[String], A]](Right(value)) {
        case (Right(_), errorMessage) => Left(NonEmptyList.of(errorMessage))

        case (Left(list), errorMessage) => Left(list.append(errorMessage))
      }

  implicit class RequestWrapper[F[_]](request: Request[F]) {
    def to[A: EntityDecoder[F, *]: Validator](implicit monadThrow: MonadThrow[F]): F[A] =
      request.as[A].flatMap {
        value =>
          FunctionKTypes.eitherToF[Throwable, F].apply {
            Validator[A].validate(value).left.map(ValidationException.apply)
          }
      }
  }
}
