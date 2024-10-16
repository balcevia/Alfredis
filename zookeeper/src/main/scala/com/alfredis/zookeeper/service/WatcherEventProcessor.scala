package com.alfredis.zookeeper.service

import com.alfredis.error.DomainError
import com.alfredis.zookeeper.config.{ZookeeperClusterState, ZookeeperConfig}
import com.alfredis.zookeeper.model.WatcherEventType.ElectionStateChange
import com.alfredis.zookeeper.model.{WatcherEvent, WatcherEventType}
import zio.stream.ZStream
import zio.{Hub, IO, Ref, ZIO, ZLayer}

case class WatcherEventProcessor(
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
      case WatcherEventType.ChildrenChange =>
        ZIO.logInfo("Got ChildrenChange event, processing...") *> processChildrenChangeEvent(event.path)
      case ElectionStateChange =>
        ZIO.logInfo("Got ElectionStateChange event, processing...") *> processElectionStateChangeEvent()
    }
  }

  private def processChildrenChangeEvent(path: String): ZIO[Any, DomainError, Unit] =
    zookeeperService
      .getChildrenWithData(path, WatcherEventType.ChildrenChange)
      .flatMap(leaders => clusterState.update(state => state.copy(leaders = leaders)))
      .tap(_ => clusterState.get.flatMap(state => ZIO.logInfo(s"Updated cluster state: $state")))

  private def processElectionStateChangeEvent(): IO[DomainError, Unit] =
    leaderElectionService.electNewLeader()
}

object WatcherEventProcessor {
  val live: ZLayer[
    Hub[WatcherEvent] & Ref[ZookeeperClusterState] & ZookeeperConfig & LeaderElectionService,
    DomainError,
    WatcherEventProcessor,
  ] = ZLayer.fromZIO {
    for {
      config                <- ZIO.service[ZookeeperConfig]
      clusterState          <- ZIO.service[Ref[ZookeeperClusterState]]
      eventHub              <- ZIO.service[Hub[WatcherEvent]]
      keeper                <- ZKConnection.connect(config.host, config.port)
      leaderElectionService <- ZIO.service[LeaderElectionService]
    } yield WatcherEventProcessor(eventHub, clusterState, ApacheZookeeperServiceImpl(keeper, eventHub), leaderElectionService)
  }
}
