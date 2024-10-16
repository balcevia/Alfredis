package com.alfredis.zookeeper.service

import com.alfredis.error.DomainError
import com.alfredis.zookeeper.config.{AppConfig, ZookeeperClusterState, ZookeeperConfig, ZookeeperNode}
import com.alfredis.zookeeper.model.WatcherEvent
import com.alfredis.zookeeper.model.WatcherEventType.ElectionStateChange
import com.alfredis.zookeeper.service
import org.apache.zookeeper.CreateMode
import zio.{Hub, IO, Ref, ZIO, ZLayer}

case class LeaderElectionServiceImpl(service: ApacheZookeeperService, clusterState: Ref[ZookeeperClusterState], appConfig: AppConfig)
    extends LeaderElectionService {
  override def startElection(): IO[DomainError, Unit] = {
    val data = "Test data for now" // fixme send host:port instead of test data
    for {
      electionNodeExists <- service.exists(appConfig.electionPath)
      _                  <- if (!electionNodeExists) service.create(appConfig.electionPath, None, CreateMode.PERSISTENT) else ZIO.unit
      createdNode        <- service.create(s"${appConfig.electionPath}/leader", Some(data), CreateMode.EPHEMERAL_SEQUENTIAL)
      _                  <- clusterState.update(state => state.copy(electionNode = Some(createdNode)))
      _                  <- electNewLeader()
    } yield ()
  }

  override def electNewLeader(): IO[DomainError, Unit] = for {
    electionState <- service.getChildren(appConfig.electionPath, true, ElectionStateChange)
    state         <- clusterState.get
    isLeader = isCurrentNodeLeader(state.electionNode.map(_.path), electionState)
    _ <- clusterState.update(state => state.copy(isLeader = isLeader, currentLeader = Some(electionState.min)))
    _ <- removeWorkerNodeIfNeeded(isLeader, state.workerNode)
  } yield ()

  private def removeWorkerNodeIfNeeded(isLeader: Boolean, workerNode: Option[ZookeeperNode]): IO[DomainError, Unit] = {
    workerNode match {
      case Some(node) if isLeader =>
        for {
          _ <- ZIO.logInfo(s"Current node was elected as a new leader, removing worker node at path '${node.path}'....")
          _ <- service.remove(node.path, node.version)
          _ <- clusterState.update(state => state.copy(workerNode = None))
        } yield ()
      case _ => ZIO.unit
    }
  }
  private def isCurrentNodeLeader(nodeName: Option[String], children: List[String]): Boolean =
    nodeName.exists(_.contains(children.min))
}

object LeaderElectionServiceImpl {
  val live: ZLayer[
    Hub[WatcherEvent] & AppConfig & Ref[ZookeeperClusterState] & ZookeeperConfig,
    DomainError,
    LeaderElectionService,
  ] =
    ZLayer.fromZIO {
      for {
        config       <- ZIO.service[ZookeeperConfig]
        clusterState <- ZIO.service[Ref[ZookeeperClusterState]]
        appConfig    <- ZIO.service[AppConfig]
        hub          <- ZIO.service[Hub[WatcherEvent]]
        keeper       <- ZKConnection.connect(config.host, config.port)
      } yield LeaderElectionServiceImpl(ApacheZookeeperServiceImpl(keeper, hub), clusterState, appConfig)
    }
}
