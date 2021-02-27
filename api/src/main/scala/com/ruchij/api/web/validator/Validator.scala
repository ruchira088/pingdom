package com.ruchij.api.web.validator

import cats.SemigroupK
import cats.data.NonEmptyList
import cats.effect.MonadThrow
import cats.implicits._
import com.ruchij.core.exceptions.ValidationException
import com.ruchij.core.types.FunctionKTypes
import org.http4s.{EntityDecoder, Request}
import shapeless.labelled.FieldType
import shapeless.{HList, HNil, LabelledGeneric, Witness, ::}

trait Validator[-A] {

  def validate[B <: A](input: B): Either[NonEmptyList[String], B]

}

object Validator {

  implicit val validatorSemigroupK: SemigroupK[Validator] =
    new SemigroupK[Validator] {
      override def combineK[A](x: Validator[A], y: Validator[A]): Validator[A] =
        new Validator[A] {
          override def validate[B <: A](input: B): Either[NonEmptyList[String], B] = {
            x.validate(input)
              .fold(
                errors => Left(y.validate(input).fold(otherErrors => errors ::: otherErrors, _ => errors)),
                _ => y.validate(input)
              )
          }
        }
    }

  def apply[A](implicit validator: Validator[A]): Validator[A] = validator

  implicit def noValidator[A]: Validator[A] =
    new Validator[A] {
      override def validate[B <: A](input: B): Either[NonEmptyList[String], B] = Right(input)
    }

  implicit def typeValidator[A, Repr <: HList](
    implicit labelledGeneric: LabelledGeneric.Aux[A, Repr],
    labelledGenericValidator: Validator[Repr]
  ): Validator[A] = new Validator[A] {
    override def validate[B <: A](input: B): Either[NonEmptyList[String], B] =
      labelledGenericValidator.validate(labelledGeneric.to(input)).as(input)
  }

  implicit val hNilValidator: Validator[HNil] = new Validator[HNil] {
    override def validate[B <: HNil](input: B): Either[NonEmptyList[String], B] = Right(input)
  }

  implicit def hListValidator[K <: Symbol, V, Tail <: HList](
    implicit witness: Witness.Aux[K],
    valueValidator: Validator[V],
    tailValidator: Validator[Tail]
  ): Validator[FieldType[K, V] :: Tail] =
    new Validator[FieldType[K, V] :: Tail] {
      override def validate[B <: FieldType[K, V] :: Tail](input: B): Either[NonEmptyList[String], B] =
        valueValidator
          .validate(input.head)
          .fold(
            errors => {
              val errorMessage = s"${witness.value.name}: ${errors.toList.mkString(", ")}"

              Left {
                tailValidator
                  .validate(input.tail)
                  .fold(rest => NonEmptyList(errorMessage, rest.toList), _ => NonEmptyList.of(errorMessage))
              }
            },
            _ => tailValidator.validate(input.tail)
          )
          .as(input)
    }

  implicit val stringValidator: Validator[String] =
    new Validator[String] {
      override def validate[B <: String](input: B): Either[NonEmptyList[String], B] =
        if (input.trim.isEmpty) Left(NonEmptyList.of("cannot be empty")) else Right(input)
    }

  implicit class RequestWrapper[F[_]](request: Request[F]) {
    def to[A: EntityDecoder[F, *]: Validator](implicit monadThrow: MonadThrow[F]): F[A] =
      request.as[A].flatMap { value =>
        FunctionKTypes.eitherToF[Throwable, F].apply {
          Validator[A].validate(value).left.map(ValidationException.apply)
        }
      }
  }

}
