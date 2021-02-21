package com.ruchij.web.routes

import cats.effect.IO
import com.ruchij.test.HttpTestResource
import com.ruchij.test.matchers.{beJsonContentType, containJson, haveJson, haveStatus}
import com.ruchij.test.mixins.IOSupport
import com.ruchij.test.utils.Providers._
import com.ruchij.test.syntax._
import io.circe.literal.JsonStringContext
import org.http4s.Method.POST
import org.http4s.{Request, Status}
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import scala.concurrent.ExecutionContext.Implicits.global

class UserRoutesSpec extends AnyFlatSpec with Matchers with IOSupport {

  "POST /user" should "create a new user" in run {
    HttpTestResource.serviceConfiguration[IO]
      .flatMap { configuration => HttpTestResource[IO](configuration).map(configuration -> _) }
      .use {
        case (configuration, http) =>
          val body =
            json"""{
              "firstName": "John",
              "lastName": "Smith",
              "email": "john.smith@email.com",
              "password": "Passw0rd123"
            }"""

          val request: Request[IO] = POST(uri"/user", body)

          for {
            response <- http.run(request)

            _ = {
              response must beJsonContentType
              response must containJson {
                json"""{ "firstName": "John", "lastName": "Smith", "email": "john.smith@email.com" }"""
              }
              response must haveStatus(Status.Created)
            }
          }
          yield (): Unit
      }
  }

}
