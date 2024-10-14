package com.alfredis.config

import pureconfig.{ConfigReader, ConfigSource}
import zio.{UIO, ZIO}

object ZIOConfigLoader {

  def loadConfigOrDie[C](namespace: String = "")(implicit decoder: ConfigReader[C]): UIO[C] =
    ZIO
      .fromEither(ConfigSource.default.at(namespace).load[C])
      .flatMapError(e => ZIO.dieMessage(s"Failed to load $namespace configuration: ${e.prettyPrint()}"))
}
