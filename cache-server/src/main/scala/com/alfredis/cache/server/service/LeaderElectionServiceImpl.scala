package com.alfredis.cache.server.service

import com.alfredis.cache.server.config.AppConfig
import com.alfredis.cache.server.service
import com.alfredis.error.DomainError
import com.alfredis.zookeepercore.ZKConnection
import com.alfredis.zookeepercore.config.{ZookeeperClusterState, ZookeeperConfig, ZookeeperNode}
import com.alfredis.zookeepercore.model.{WatcherEvent, WatcherEventType}
import com.alfredis.zookeepercore.service.{ApacheZookeeperService, ApacheZookeeperServiceImpl}
import org.apache.zookeeper.CreateMode
import zio.{Hub, IO, Ref, ZIO, ZLayer}

case class LeaderElectionServiceImpl(
    service: ApacheZookeeperService,
    clusterState: Ref[ZookeeperClusterState],
    zookeeperConfig: ZookeeperConfig,
    appConfig: AppConfig,
) extends LeaderElectionService {
  override def startElection(): IO[DomainError, Unit] = {
    val data = ZookeeperNode.encodeData(appConfig.groupName, appConfig.server.externalHostName)
    for {
      _           <- ZIO.logTrace("Joining elections....")
      _           <- createNode(zookeeperConfig.electionPath, CreateMode.PERSISTENT)
      createdNode <- service.create(s"${zookeeperConfig.electionPath}/leader", Some(data), CreateMode.EPHEMERAL_SEQUENTIAL)
      _           <- clusterState.update(state => state.copy(electionNode = Some(createdNode)))
      _           <- electNewLeader()
    } yield ()
  }

  override def electNewLeader(): IO[DomainError, Unit] = for {
    _             <- ZIO.logTrace("Electing new leader...")
    electionState <- service.getChildren(zookeeperConfig.electionPath, Some(WatcherEventType.ElectionStateChange))
    state         <- clusterState.get
    isLeader = isCurrentNodeLeader(state.electionNode.map(_.path), electionState)
    _ <- createNode(zookeeperConfig.leadersPath, CreateMode.PERSISTENT)
    _ <-
      if (isLeader)
        service.create(
          s"${zookeeperConfig.leadersPath}/leader",
          Some(ZookeeperNode.encodeData(appConfig.groupName, appConfig.server.externalHostName)),
          CreateMode.EPHEMERAL_SEQUENTIAL,
        )
      else ZIO.unit
    _ <- retrieveLeadersDataIfNeeded(electionState.min, isLeader)
    _ <- removeWorkerNodeIfNeeded(isLeader, state.workerNode)
    _ <- retrieveWorkersListIfNeeded(isLeader)
  } yield ()

  private def createNode(path: String, mode: CreateMode): ZIO[Any, DomainError, Unit] = {
    for {
      nodeExists <- service.exists(path)
      _          <- if (!nodeExists) service.create(path, None, mode) else ZIO.unit
    } yield ()
  }

  private def retrieveLeadersDataIfNeeded(leader: String, isLeader: Boolean): IO[DomainError, Unit] = {
    service.getData(s"${zookeeperConfig.electionPath}/$leader").flatMap { leaderNode =>
      clusterState.update(_.copy(isLeader = isLeader, currentLeader = Some(leaderNode)))
    }
  }

  private def removeWorkerNodeIfNeeded(isLeader: Boolean, workerNode: Option[ZookeeperNode]): IO[DomainError, Unit] = {
    workerNode match {
      case Some(node) if isLeader =>
        for {
          _ <- ZIO.logTrace(s"Current node was elected as a new leader, removing worker node at path '${node.path}'....")
          _ <- service.remove(node.path, node.version)
          _ <- clusterState.update(state => state.copy(workerNode = None))
        } yield ()
      case _ => ZIO.unit
    }
  }

  private def retrieveWorkersListIfNeeded(isLeader: Boolean) = {
    if (isLeader) {
      for {
        _                 <- ZIO.logTrace("Retrieving workers list...")
        workersNodeExists <- service.exists(zookeeperConfig.workersPath)
        _                 <- if (!workersNodeExists) service.create(zookeeperConfig.workersPath, None, CreateMode.PERSISTENT) else ZIO.unit
        children          <- service.getChildrenWithData(zookeeperConfig.workersPath, Some(WatcherEventType.WorkersChange))
        _                 <- ZIO.logTrace(s"Retrieved workers: $children")
        _                 <- clusterState.update(state => state.copy(workers = children.values.toList))
      } yield ()
    } else ZIO.succeed(())
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
      } yield LeaderElectionServiceImpl(ApacheZookeeperServiceImpl(keeper, hub), clusterState, config, appConfig)
    }
}
