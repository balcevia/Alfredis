package com.alfredis.zookeepercore.config

import com.alfredis.config.ZIOConfigLoader.loadConfigOrDie
import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*
import zio.{ULayer, ZLayer}

case class ZookeeperConfig(host: String, port: Int, leadersPath: String, electionPath: String, workersPath: String)

object ZookeeperConfig {
  given ConfigReader[ZookeeperConfig] = ConfigReader.derived

  val live: ULayer[ZookeeperConfig] =
    ZLayer(loadConfigOrDie[ZookeeperConfig]("zookeeper"))
}
