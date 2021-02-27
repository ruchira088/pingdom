package com.ruchij.core.daos.ping.models

import org.http4s.{Headers, Method, Uri}
import org.joda.time.DateTime

import scala.concurrent.duration.FiniteDuration

case class Ping(
  id: String,
  accountId: String,
  createdAt: DateTime,
  modifiedAt: DateTime,
  uri: Uri,
  method: Method,
  headers: Headers,
  body: Option[String],
  frequency: FiniteDuration
)
