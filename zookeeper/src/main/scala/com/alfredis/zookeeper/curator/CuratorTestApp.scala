package com.alfredis.zookeeper.curator

import com.alfredis.zookeeper.config.{AppConfig, ZookeeperClusterState, ZookeeperConfig}
import com.alfredis.zookeeper.model.WatcherEvent
import zio.{Hub, Ref, Scope, ULayer, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

object CuratorTestApp extends ZIOAppDefault {
  val hubLayer: ULayer[Hub[WatcherEvent]] = ZLayer.fromZIO {
    Hub.unbounded[WatcherEvent]()
  }

  private val mainFlow: ZIO[StartUpService, Nothing, Unit] = for {
    service <- ZIO.service[StartUpService]
    _       <- service.startup()
    _       <- ZIO.never
  } yield ()

  private val eventProcessor: ZIO[WatcherEventProcessor, Nothing, Unit] = for {
    eventProcessor <- ZIO.service[WatcherEventProcessor]
    _              <- eventProcessor.subscribe()
  } yield ()

  override def run: ZIO[Any & ZIOAppArgs & Scope, Any, Any] =
    ZIO
      .collectAllPar(List(mainFlow, eventProcessor))
      .provide(
        hubLayer,
        ZookeeperConfig.live,
        ZookeeperClusterState.live,
        LeadersWatcher.live,
        AppConfig.live,
        StartUpServiceImpl.live,
        WatcherEventProcessor.live,
      )
}
