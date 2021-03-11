package com.ruchij.api.web.requests

import org.http4s.{Header, Method, Uri}

import scala.concurrent.duration.FiniteDuration

case class NewPingRequest(
  uri: Uri,
  method: Method,
  headers: List[Header],
  body: Option[String],
  frequency: FiniteDuration
)
