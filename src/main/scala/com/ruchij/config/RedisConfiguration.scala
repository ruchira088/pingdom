package com.ruchij.config

case class RedisConfiguration(host: String, port: Int, password: Option[String]) {
  val url: String = ???
}
