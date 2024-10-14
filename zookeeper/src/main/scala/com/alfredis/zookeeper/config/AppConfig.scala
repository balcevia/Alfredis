package com.alfredis.zookeeper.config

import com.alfredis.config.ZIOConfigLoader.loadConfigOrDie
import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*
import zio.{ULayer, ZLayer}

case class AppConfig(leadersPath: String)

object AppConfig {
  given ConfigReader[AppConfig] = ConfigReader.derived

  val live: ULayer[AppConfig] =
    ZLayer(loadConfigOrDie[AppConfig]("app"))
}
