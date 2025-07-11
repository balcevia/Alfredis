package com.alfredis.cache.server

import com.alfredis.cache.Cache
import com.alfredis.cache.server.config.AppConfig
import com.alfredis.cache.server.http.CacheHttpServer
import com.alfredis.cache.server.http.service.{CacheService, CacheServiceImpl}
import com.alfredis.cache.server.service.*
import com.alfredis.cache.server.tcp.ZIOTCPServer
import com.alfredis.error.DomainError
import com.alfredis.tcp.ZIOTCPClient
import com.alfredis.zookeepercore.config.{ZookeeperClusterState, ZookeeperConfig}
import com.alfredis.zookeepercore.model.WatcherEvent
import zio.logging.backend.SLF4J
import zio.{Hub, Ref, Scope, ULayer, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

object CacheServerApp extends ZIOAppDefault {

  override def run: ZIO[Any & ZIOAppArgs & Scope, Any, Any] =
    ZIO
      .collectAllPar(List(ZookeeperServer.run(), EventProcessor.run(), ZIO.serviceWithZIO[ZIOTCPServer](_.start()), CacheCleaner.run(), CacheHttpServer.serve))
      .provide(
        CommonLayers.eventHubLayer,
        ZookeeperConfig.live,
        ZookeeperClusterState.live,
        AppConfig.live,
        WatcherEventProcessor.live,
        LeaderElectionServiceImpl.live,
        WorkerRegistrationServiceImpl.live,
        CacheServiceImpl.live,
        CommonLayers.cacheLayer,
        zio.Runtime.removeDefaultLoggers >>> SLF4J.slf4j,
        CacheCleaner.live,
        ZIOTCPClient.live,
        ZIOTCPServer.live,
      )
}
