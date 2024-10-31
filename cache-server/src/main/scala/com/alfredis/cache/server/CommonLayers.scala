package com.alfredis.cache.server

import com.alfredis.cache.server.config.AppConfig
import com.alfredis.cache.{Cache, LRUCache}
import com.alfredis.zookeepercore.model.WatcherEvent
import zio.{Hub, Ref, ULayer, ZIO, ZLayer}

object CommonLayers {
  val eventHubLayer: ULayer[Hub[WatcherEvent]] = ZLayer.fromZIO {
    Hub.unbounded[WatcherEvent]()
  }

  val cacheLayer: ZLayer[AppConfig, Nothing, Ref[Cache[String, Array[Byte]]]] = ZLayer.fromZIO {
    ZIO.service[AppConfig].flatMap { config =>
      Ref.make[Cache[String, Array[Byte]]](new LRUCache[String, Array[Byte]](config.cache.capacity, config.cache.ttl))
    }
  }
}
