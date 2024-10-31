package com.alfredis.cache.server

import com.alfredis.cache.Cache
import com.alfredis.cache.server.config.AppConfig
import zio.{Duration, Ref, Schedule, ZIO, ZLayer}

case class CacheCleaner(cache: Ref[Cache[String, Array[Byte]]], appConfig: AppConfig) {
  def run(): ZIO[Any, Nothing, Long] =
    (ZIO.logInfo("Running cleaning operation") *>
      cache
        .getAndUpdate { c =>
          c.removeOutdatedEntries(); c
        } *> ZIO.logInfo("Cache cleaned"))
      .repeat(Schedule.fixed(Duration.fromSeconds(appConfig.cache.ttl)))
}

object CacheCleaner {
  val live: ZLayer[AppConfig & Ref[Cache[String, Array[Byte]]], Nothing, CacheCleaner] = ZLayer.fromZIO {
    for {
      cache  <- ZIO.service[Ref[Cache[String, Array[Byte]]]]
      config <- ZIO.service[AppConfig]
    } yield CacheCleaner(cache, config)
  }

  def run(): ZIO[CacheCleaner, Nothing, Unit] = for {
    cleaner <- ZIO.service[CacheCleaner]
    _       <- cleaner.run()
  } yield ()
}
