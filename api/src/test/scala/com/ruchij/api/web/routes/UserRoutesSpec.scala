package com.ruchij.api.web.routes

import cats.effect.{Blocker, IO}
import com.ruchij.core.daos.doobie.DoobieTransactor
import com.ruchij.core.daos.user.DoobieUserDao
import com.ruchij.core.daos.user.models.Email
import com.ruchij.api.test.HttpTestResource
import com.ruchij.api.test.matchers.{beJsonContentType, containJson, haveStatus}
import com.ruchij.test.mixins.IOSupport
import com.ruchij.api.test.syntax._
import com.ruchij.test.utils.Providers._
import com.ruchij.core.types.CustomBlocker.IOBlocker
import doobie.implicits._
import io.circe.literal.JsonStringContext
import org.http4s.Method.POST
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Request, Status}
import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class UserRoutesSpec extends AnyFlatSpec with Matchers with IOSupport with OptionValues {

  "POST /user" should "create a new user" in run {
    HttpTestResource
      .apiConfiguration[IO]
      .flatMap { configuration =>
        HttpTestResource[IO](configuration).map(configuration -> _)
      }
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
              response must haveStatus(Status.Created)
              response must containJson {
                json"""{ "firstName": "John", "lastName": "Smith", "email": "john.smith@email.com" }"""
              }
            }

            transactor <- DoobieTransactor.create[IO](
              configuration.databaseConfiguration,
              IOBlocker(Blocker.liftExecutionContext(ExecutionContext.global))
            )

            maybeUser <- DoobieUserDao.findByEmail(Email("john.smith@email.com")).transact(transactor)

            _ = {
              maybeUser.value.firstName mustBe "John"
              maybeUser.value.lastName mustBe "Smith"
              maybeUser.value.email mustBe Email("john.smith@email.com")
            }

          } yield (): Unit
      }
  }

}
