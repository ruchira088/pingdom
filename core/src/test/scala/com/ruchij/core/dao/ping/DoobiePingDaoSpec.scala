package com.ruchij.core.dao.ping

import cats.effect.{Blocker, IO}
import cats.implicits._
import com.ruchij.core.daos.account.DoobieAccountDao
import com.ruchij.core.daos.account.models.Account
import com.ruchij.core.daos.doobie.DoobieTransactor
import com.ruchij.core.daos.ping.DoobiePingDao
import com.ruchij.core.daos.ping.models.Ping
import com.ruchij.core.test.h2DatabaseConfiguration
import com.ruchij.core.test.mixins.IOSupport
import com.ruchij.core.test.utils.Providers._
import com.ruchij.core.types.CustomBlocker.IOBlocker
import com.ruchij.core.types.{JodaClock, RandomGenerator}
import com.ruchij.migration.MigrationApp
import org.http4s.headers.{Authorization, `Content-Type`}
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{BasicCredentials, Headers, MediaType, Method}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class DoobiePingDaoSpec extends AnyFlatSpec with Matchers with IOSupport {

  "DoobiePingDao" should "perform CRUD operations" in run {
    RandomGenerator[IO, UUID]
      .generate[UUID]
      .map(uuid => h2DatabaseConfiguration(uuid.toString))
      .flatTap(databaseConfiguration => MigrationApp.migrate[IO](databaseConfiguration))
      .flatMap { database =>
        DoobieTransactor.create[IO](database, IOBlocker(Blocker.liftExecutionContext(ExecutionContext.global)))
      }
      .map(_.trans)
      .flatMap { implicit transaction =>
        for {
          timestamp <- JodaClock[IO].currentTimestamp

          account = Account("account-id", timestamp, timestamp, "My Account")
          ping = Ping(
            "ping-id",
            account.id,
            timestamp,
            timestamp,
            uri"https://ip.ruchij.com",
            Method.POST,
            Headers
              .of(`Content-Type`(MediaType.application.json), Authorization(BasicCredentials("admin", "password"))),
            Some {
              """{ "name": "John", "age": 20 }"""
            },
            FiniteDuration(30, TimeUnit.SECONDS)
          )

          _ <- transaction.apply {
            DoobieAccountDao
              .save(Account("account-id", timestamp, timestamp, "My Account"))
              .product(DoobiePingDao.save(ping))
          }

          fetchedPingById <- transaction.apply(DoobiePingDao.findById(ping.id))
          fetchedPingsByAccountId <- transaction.apply(DoobiePingDao.findByAccount(ping.accountId))

          _ = {
            fetchedPingById mustBe Some(ping)
            fetchedPingsByAccountId mustBe List(ping)
          }

        } yield (): Unit
      }
  }
}
