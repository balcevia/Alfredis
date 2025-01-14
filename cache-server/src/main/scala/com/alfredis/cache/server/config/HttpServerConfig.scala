package com.alfredis.cache.server.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

case class HttpServerConfig(host: String, port: Int, int: String) {
  val hostName: String = s"$int:$port"
}

object HttpServerConfig:
  given ConfigReader[HttpServerConfig] = ConfigReader.derived
