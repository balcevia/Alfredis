package com.alfredis

import com.alfredis.cache.client.AlfredisCacheClientImpl
import com.alfredis.console.client.{CommandProcessor, ConsoleCacheClient}
import com.alfredis.tcp.ZIOTCPClient
import com.alfredis.zookeepercore.config.ZookeeperConfig
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object AlfredisApp extends ZIOAppDefault {
  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] = {
    val program = for {
      client <- ZIO.service[ConsoleCacheClient]
      _      <- client.run
    } yield ()

    program.provide(ConsoleCacheClient.live, AlfredisCacheClientImpl.live, ZookeeperConfig.live, ZIOTCPClient.live, CommandProcessor.live)
  }
}
