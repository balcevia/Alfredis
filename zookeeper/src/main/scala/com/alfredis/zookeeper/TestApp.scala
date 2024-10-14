package com.alfredis.zookeeper

import com.alfredis.error.DomainError
import com.alfredis.zookeeper.config.{AppConfig, ZookeeperClusterState, ZookeeperConfig}
import com.alfredis.zookeeper.model.WatcherEvent
import com.alfredis.zookeeper.service.{StartUpService, StartUpServiceImpl, WatcherEventProcessor}
import zio.{Hub, Ref, Scope, ULayer, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

object TestApp extends ZIOAppDefault {
  val hubLayer: ULayer[Hub[WatcherEvent]] = ZLayer.fromZIO {
    Hub.unbounded[WatcherEvent]()
  }

  private val mainFlow: ZIO[StartUpService, DomainError, Unit] = for {
    service <- ZIO.service[StartUpService]
    _       <- service.startup()
    _       <- ZIO.never
  } yield ()

  private val eventProcessor: ZIO[WatcherEventProcessor, DomainError, Unit] = for {
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
        AppConfig.live,
        StartUpServiceImpl.live,
        WatcherEventProcessor.live,
      )
}
