package com.alfredis.cache.server.service

import com.alfredis.cache.server.config.AppConfig
import com.alfredis.error.DomainError
import com.alfredis.zookeepercore.ZKConnection
import com.alfredis.zookeepercore.config.{ZookeeperClusterState, ZookeeperConfig}
import com.alfredis.zookeepercore.model.{WatcherEvent, WatcherEventType}
import com.alfredis.zookeepercore.service.{ApacheZookeeperService, ApacheZookeeperServiceImpl}
import zio.stream.ZStream
import zio.{Hub, IO, Ref, ZIO, ZLayer}

case class WatcherEventProcessor(
    zookeeperConfig: ZookeeperConfig,
    eventHub: Hub[WatcherEvent],
    clusterState: Ref[ZookeeperClusterState],
    zookeeperService: ApacheZookeeperService,
    leaderElectionService: LeaderElectionService,
) {

  def subscribe(): ZIO[Any, DomainError, Unit] = {
    val stream = ZStream.fromHub(eventHub)

    stream.foreach(processEvent)
  }

  private def processEvent(event: WatcherEvent): ZIO[Any, DomainError, Unit] = {
    event.eventType match {
      case WatcherEventType.ElectionStateChange =>
        ZIO.logTrace("Got ElectionStateChange event, processing...") *> processElectionStateChangeEvent()
      case WatcherEventType.WorkersChange =>
        ZIO.logTrace("Got WorkersChange event, processing...") *> processWorkersChangeEvent()
    }
  }

  private def processWorkersChangeEvent(): ZIO[Any, DomainError, Unit] =
    zookeeperService
      .getChildrenWithData(zookeeperConfig.workersPath, Some(WatcherEventType.WorkersChange))
      .flatMap(workers => clusterState.update(state => state.copy(workers = workers.values.toList)))
      .tap(_ => clusterState.get.flatMap(state => ZIO.logTrace(s"Updated cluster state: $state")))

  private def processElectionStateChangeEvent(): IO[DomainError, Unit] =
    leaderElectionService.electNewLeader()
}

object WatcherEventProcessor {
  val live: ZLayer[
    Hub[WatcherEvent] & Ref[ZookeeperClusterState] & ZookeeperConfig & LeaderElectionService & AppConfig,
    DomainError,
    WatcherEventProcessor,
  ] = ZLayer.fromZIO {
    for {
      config                <- ZIO.service[ZookeeperConfig]
      clusterState          <- ZIO.service[Ref[ZookeeperClusterState]]
      eventHub              <- ZIO.service[Hub[WatcherEvent]]
      keeper                <- ZKConnection.connect(config.host, config.port)
      leaderElectionService <- ZIO.service[LeaderElectionService]
    } yield WatcherEventProcessor(config, eventHub, clusterState, ApacheZookeeperServiceImpl(keeper, eventHub), leaderElectionService)
  }
}
