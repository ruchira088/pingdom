package com.ruchij.core.daos.ping

import com.ruchij.core.daos.ping.models.Ping
import com.ruchij.core.daos.doobie.DoobieCustomMappings._
import cats.implicits._
import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator
import doobie.util.fragment.Fragment
import org.http4s.{Header, Headers, Method, Uri}
import org.joda.time.DateTime

import scala.concurrent.duration.FiniteDuration

object DoobiePingDao extends PingDao[ConnectionIO] {

  def selectQuery(condition: Fragment): ConnectionIO[List[Ping]] =
    (sql"""
      SELECT 
        ping.id, ping.created_at, ping.modified_at, ping.account_id, ping.uri, ping.method, ping_body.body, ping.frequency
      FROM ping
      JOIN ping_body ON ping.id = ping_body.ping_id
    """ ++ condition)
      .query[(String, DateTime, DateTime, String, Uri, Method, Option[String], FiniteDuration)]
      .to[List]
      .flatMap {
        _.traverse {
          case (id, createdAt, modifiedAt, accountId, uri, method, body, frequency) =>
            sql"SELECT name, value FROM ping_header WHEN ping_id = $id"
              .query[(String, String)]
              .to[List]
              .map { headers =>
                headers.map { case (name, value) => Header(name, value) }
              }
              .map { headers =>
                Ping(id, accountId, createdAt, modifiedAt, uri, method, Headers(headers), body, frequency)
              }
        }
      }

  override def save(ping: Ping): ConnectionIO[Int] =
    sql"""
      INSERT INTO ping (id, created_at, modified_at, account_id, uri, method, frequency)
        VALUES (
          ${ping.id}, 
          ${ping.createdAt}, 
          ${ping.modifiedAt}, 
          ${ping.accountId}, 
          ${ping.uri}, 
          ${ping.method},
          ${ping.frequency}
        )
    """.update.run
      .product {
        sql"""
          INSERT INTO ping_body (ping_id, created_at, modified_at, body)
            VALUES (${ping.id},  ${ping.createdAt}, ${ping.modifiedAt}, ${ping.body})
        """.update.run
      }
      .product {
        ping.headers.toList.traverse { header =>
          sql"""
              INSERT INTO ping_header (created_at, modified_at, ping_id, name, value)
                VALUES (${ping.createdAt}, ${ping.modifiedAt}, ${ping.id}, ${header.name.value}, ${header.value})
            """.update.run
        }
      }
      .map {
        case ((pingResult, pingBodyResult), headerResults) => pingResult + pingBodyResult + headerResults.sum
      }

  override def findByAccount(accountId: String): ConnectionIO[List[Ping]] =
    selectQuery(fr"WHERE account_id = $accountId")

  override def findById(pingId: String): ConnectionIO[Option[Ping]] =
    selectQuery(fr"WHERE id = $pingId").map(_.headOption)

}
