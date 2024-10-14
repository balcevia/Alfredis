package com.alfredis.zookeeper

import com.alfredis.error.ZookeeperConnectionError
import com.alfredis.zookeeper.config.{ZookeeperClusterState, ZookeeperConfig}
import com.alfredis.zookeeper.model.WatcherEvent
import com.alfredis.zookeeper.service.{WatcherEventProcessor, ZKConnection, ZKZioService, ZKZioServiceImpl}
import org.apache.zookeeper.ZooKeeper
import zio.{Hub, Ref, Scope, ULayer, URLayer, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

object TestApp extends ZIOAppDefault {

  val keeperLayer: ZLayer[ZookeeperConfig, ZookeeperConnectionError, ZooKeeper] = ZLayer.fromZIO {
    for {
      config <- ZIO.service[ZookeeperConfig]
      keeper <- ZKConnection.connect(config.host, config.port)
    } yield keeper
  }

  val hubLayer: ULayer[Hub[WatcherEvent]] = ZLayer.fromZIO {
    Hub.unbounded[WatcherEvent]()
  }

  import zio.durationInt

  val applicationEffect = for {
    service      <- ZIO.service[ZKZioService]
    hub          <- ZIO.service[Hub[WatcherEvent]]
    clusterState <- ZIO.service[Ref[ZookeeperClusterState]]
    ch           <- service.getChildren("/leaders", true)
    _            <- clusterState.get().flatMap(state => ZIO.logInfo(s"CURRENT STATE: $state"))
    _            <- ZIO.sleep(30.seconds) *> clusterState.get().flatMap(state => ZIO.logInfo(s"UPDATED STATE: $state"))
    _            <- ZIO.never
  } yield ()

  val watcherEffect = for {
    watcher <- ZIO.service[WatcherEventProcessor]
    _       <- watcher.subscribe()
  } yield ()

  val app = applicationEffect &> watcherEffect

  override def run: ZIO[Any & ZIOAppArgs & Scope, Any, Any] = app
    .provide(
      ZookeeperConfig.live,
      keeperLayer,
      ZookeeperClusterState.live,
      hubLayer,
      ZKZioServiceImpl.live,
      WatcherEventProcessor.live,
    )
    .exitCode

  // todo fix problem with retrieving all children info
  // todo fix problem with children change watch
}
