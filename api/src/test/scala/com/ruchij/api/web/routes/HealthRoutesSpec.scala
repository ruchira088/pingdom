package com.ruchij.api.web.routes

import cats.effect.{Clock, IO, Resource}
import com.eed3si9n.ruchij.api.BuildInfo
import com.ruchij.api.circe.Encoders.dateTimeEncoder
import com.ruchij.api.test.HttpTestResource
import com.ruchij.api.test.matchers._
import com.ruchij.core.test.mixins.IOSupport
import com.ruchij.core.test.utils.Providers._
import io.circe.literal._
import org.http4s.Method.GET
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{HttpApp, Request, Response, Status}
import org.joda.time.DateTime
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Properties

class HealthRoutesSpec extends AnyFlatSpec with Matchers with IOSupport {

  "GET /service/info" should "return a successful response containing service information" in {
    val dateTime = DateTime.now()
    implicit val clock: Clock[IO] = stubClock[IO](dateTime)

    val httpResource: Resource[IO, HttpApp[IO]] = HttpTestResource[IO]

    val request = Request[IO](GET, uri"/service/info")

    val response: Response[IO] = run { httpResource.use(_.run(request)) }

    val expectedJsonResponse =
      json"""{
        "serviceName": "pingdom-api",
        "serviceVersion": ${BuildInfo.version},
        "organization": "com.ruchij",
        "scalaVersion": "2.13.5",
        "sbtVersion": "1.4.9",
        "gitBranch" : "test-branch",
        "gitCommit" : "my-commit",
        "javaVersion": ${Properties.javaVersion},
        "gitBranch" : "test-branch",
        "gitCommit" : "my-commit",
        "buildTimestamp" : null,
        "timestamp": $dateTime
      }"""

    response must beJsonContentType
    response must haveStatus(Status.Ok)
    response must haveJson(expectedJsonResponse)
  }

  "GET /service/health-check" should "return perform a health check" in {
    val httpResource: Resource[IO, HttpApp[IO]] = HttpTestResource[IO]

    val request = Request[IO](GET, uri"/service/health-check")

    val response: Response[IO] = run { httpResource.use(_.run(request)) }

    val expectedJsonResponse =
      json"""{
        "database" : "Healthy",
        "keyValueStore" : "Healthy"
      }"""

    response must beJsonContentType
    response must haveStatus(Status.Ok)
    response must haveJson(expectedJsonResponse)
  }
}
