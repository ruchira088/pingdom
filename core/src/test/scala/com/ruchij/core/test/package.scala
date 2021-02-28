package com.ruchij.core

import cats.MonadError
import cats.implicits._
import cats.effect.{Resource, Sync}
import com.ruchij.core.types.RandomGenerator
import com.ruchij.migration.config.DatabaseConfiguration
import redis.embedded.RedisServer

import java.net.ServerSocket

package object test {
  def startEmbeddedRedis[F[_]: Sync](port: Int): Resource[F, RedisServer] =
    Resource
      .pure[F, RedisServer](RedisServer.builder().port(port).build())
      .flatTap { redisServer =>
        Resource.make(Sync[F].delay(redisServer.start()))(_ => Sync[F].delay(redisServer.stop()))
      }

  def h2DatabaseConfiguration(databaseName: String): DatabaseConfiguration =
    DatabaseConfiguration(
      s"jdbc:h2:mem:$databaseName;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
      "",
      ""
    )

  def availablePort[F[_]: Sync](start: Int): F[Int] =
    RandomGenerator[F, Int].generate.map(diff => start + (diff % 1000)).flatMap { port =>
      MonadError[F, Throwable].handleErrorWith {
        Sync[F]
          .delay(new ServerSocket(port))
          .flatMap(serverSocket => Sync[F].delay(serverSocket.close()))
          .as(port)
      } { _ =>
        availablePort[F](start)
      }
    }
}
