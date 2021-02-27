package com.ruchij.core.config

case class RedisConfiguration(host: String, port: Int, password: Option[String]) {
  val url: String = s"redis://${password.fold("")(_ + "@")}$host:$port"
}
