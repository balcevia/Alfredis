package com.alfredis.zookeeper.curator

import com.alfredis.error.DomainError
import com.alfredis.zookeeper.config.{ZookeeperClusterState, ZookeeperConfig}
import com.alfredis.zookeeper.model.{WatcherEvent, WatcherEventType}
import com.alfredis.zookeeper.service.ZKConnection
import zio.stream.ZStream
import zio.{Hub, Ref, ZIO, ZLayer}

case class WatcherEventProcessor(
    eventHub: Hub[WatcherEvent],
    clusterState: Ref[ZookeeperClusterState],
    zookeeperService: ZookeeperService,
) {

  def subscribe(): ZIO[Any, Nothing, Unit] = {
    val stream = ZStream.fromHub(eventHub)

    stream.foreach(processEvent)
  }

  private def processEvent(event: WatcherEvent): ZIO[Any, Nothing, Unit] = {
    event.eventType match {
      case WatcherEventType.ChildrenChange =>
        ZIO.logInfo("Got ChildrenChange event, processing...") *> processChildrenChangeEvent(event.path)
    }
  }

  private def processChildrenChangeEvent(path: String) =
    zookeeperService
      .getChildrenWithData(path)
      .flatMap(leaders => clusterState.update(state => state.copy(leaders = leaders)))
      .tap(_ => clusterState.get.flatMap(state => ZIO.logInfo(s"Updated cluster state: $state")))
}

object WatcherEventProcessor {
  val live: ZLayer[Hub[WatcherEvent] & Ref[ZookeeperClusterState] & ZookeeperConfig, DomainError, WatcherEventProcessor] = ZLayer.fromZIO {
    for {
      config <- ZIO.service[ZookeeperConfig]
//      asyncClient  <- ZookeeperConnection.create(config)
      clusterState <- ZIO.service[Ref[ZookeeperClusterState]]
      eventHub     <- ZIO.service[Hub[WatcherEvent]]
      keeper       <- ZKConnection.connect(config.host, config.port)
    } yield WatcherEventProcessor(eventHub, clusterState, ApacheZookeeperService(ApacheZookeeperServiceImpl(keeper, eventHub)))
  }
}
