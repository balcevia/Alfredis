package com.alfredis.cache.server.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

case class CacheConfig(capacity: Int)

object CacheConfig:
  given ConfigReader[CacheConfig] = ConfigReader.derived
