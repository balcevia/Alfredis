package com.alfredis.zookeeper.service

import com.alfredis.error.DomainError
import com.alfredis.zookeeper.config.{ZookeeperClusterState, ZookeeperNode}
import com.alfredis.zookeeper.model.WatcherEvent
import com.alfredis.zookeeper.model.WatcherEventType.ChildrenChange
import zio.{Hub, Ref, URLayer, ZIO, ZLayer}

case class WatcherEventProcessor(
    zkService: ZKZioService,
    clusterState: Ref[ZookeeperClusterState],
    events: Hub[WatcherEvent],
) {

  def subscribe(): ZIO[Any, DomainError, Unit] = ZIO.scoped {
    for {
      topic <- events.subscribe
      _     <- topic.take.flatMap(processEvent)
    } yield ()
  }

  private def processEvent(event: WatcherEvent): ZIO[Any, DomainError, Unit] = {
    event.eventType match {
      case ChildrenChange => ZIO.logInfo("Got ChildrenChange event") *> processChildrenChangeEvent(event.path)
    }
  }

  private def processChildrenChangeEvent(path: String): ZIO[Any, DomainError, Unit] = {
    zkService
      .getChildren(path, true)
      .flatMap(children => ZIO.collectAllPar(children.map(child => zkService.get(child).map(data => child -> data))))
      .flatMap(data =>
        ZIO.collectAllPar(
          data.map {
            case (path, Some(data)) =>
              val deserializedData = new String(data)
              clusterState.update(state => state.copy(leaders = state.leaders + (path -> ZookeeperNode(path, deserializedData))))
            case (path, None) => ZIO.unit
          },
        ),
      )
      .tapError(error => ZIO.logError(s"processChildrenChangeEvent failed with error: ${error.message}"))
      .unit
  }
}

object WatcherEventProcessor {
  def live: URLayer[Hub[WatcherEvent] & Ref[ZookeeperClusterState] & ZKZioService, WatcherEventProcessor] = ZLayer.fromZIO {
    for {
      zkService    <- ZIO.service[ZKZioService]
      clusterState <- ZIO.service[Ref[ZookeeperClusterState]]
      hub          <- ZIO.service[Hub[WatcherEvent]]
    } yield WatcherEventProcessor(zkService, clusterState, events = hub)
  }
}
