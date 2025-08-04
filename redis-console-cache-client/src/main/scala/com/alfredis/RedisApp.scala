package com.alfredis

import com.alfredis.cache.client.RedisCacheClient
import com.alfredis.console.client.{CommandProcessor, ConsoleCacheClient}
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object RedisApp extends ZIOAppDefault {
  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] = {
    val program = for {
      client <- ZIO.service[ConsoleCacheClient]
      _      <- client.run
    } yield ()

    program.provide(ConsoleCacheClient.live, RedisCacheClient.live, CommandProcessor.live)
  }
}
