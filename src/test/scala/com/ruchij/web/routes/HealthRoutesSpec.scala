package com.ruchij.web.routes

import cats.effect.{Clock, IO, Resource}
import com.eed3si9n.ruchij.BuildInfo
import com.ruchij.circe.Encoders.dateTimeEncoder
import com.ruchij.test.HttpTestResource
import com.ruchij.test.utils.Providers._
import com.ruchij.test.matchers._
import io.circe.literal._
import org.http4s.{HttpApp, Request, Response, Status, Uri}
import org.joda.time.DateTime
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import scala.util.Using

import scala.util.Properties
import scala.concurrent.ExecutionContext.Implicits.global

class HealthRoutesSpec extends AnyFlatSpec with Matchers {
  "GET /service" should "return a successful response containing service information" in {
    val dateTime = DateTime.now()
    implicit val clock: Clock[IO] = stubClock[IO](dateTime)

    val httpResource: Resource[IO, HttpApp[IO]] = HttpTestResource[IO]

    val request = Request[IO](uri = Uri(path = "/health"))

    val response: Response[IO] = httpResource.use(_.run(request)).unsafeRunSync()

    val expectedJsonResponse =
      json"""{
        "serviceName": "pingdom",
        "serviceVersion": ${BuildInfo.version},
        "organization": "com.ruchij",
        "scalaVersion": "2.13.4",
        "sbtVersion": "1.4.7",
        "gitBranch" : "test-branch",
        "gitCommit" : "my-commit",
        "javaVersion": ${Properties.javaVersion},
        "gitBranch" : "test-branch",
        "gitCommit" : "my-commit",
        "buildTimestamp" : null,
        "timestamp": $dateTime
      }"""

    response must beJsonContentType
    response must haveJson(expectedJsonResponse)
    response must haveStatus(Status.Ok)
  }
}
