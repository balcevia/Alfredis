package com.alfredis.cache.server.config

import com.alfredis.config.ZIOConfigLoader.loadConfigOrDie
import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*
import zio.{ULayer, ZLayer}

case class AppConfig(
                      leadersPath: String,
                      electionPath: String,
                      workersPath: String,
                      server: HttpServerConfig,
                      cache: CacheConfig,
                    )

object AppConfig {
  given ConfigReader[AppConfig] = ConfigReader.derived

  val live: ULayer[AppConfig] =
    ZLayer(loadConfigOrDie[AppConfig]("app"))
}
