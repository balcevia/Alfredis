package com.alfredis.zookeeper.config

import com.alfredis.config.ZIOConfigLoader.loadConfigOrDie
import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*
import zio.{ULayer, ZLayer}

case class ZookeeperConfig(host: String, port: Int, maxRetries: Int = 3, sleepMsBetweenRetries: Int = 100)

object ZookeeperConfig {
  given ConfigReader[ZookeeperConfig] = ConfigReader.derived

  val live: ULayer[ZookeeperConfig] =
    ZLayer(loadConfigOrDie[ZookeeperConfig]("zookeeper"))
}