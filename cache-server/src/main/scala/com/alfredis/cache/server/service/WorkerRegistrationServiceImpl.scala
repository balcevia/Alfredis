package com.alfredis.cache.server.service

import com.alfredis.cache.server.config.AppConfig
import com.alfredis.error.DomainError
import com.alfredis.zookeepercore.ZKConnection
import com.alfredis.zookeepercore.config.{ZookeeperClusterState, ZookeeperConfig, ZookeeperNode}
import com.alfredis.zookeepercore.model.WatcherEvent
import com.alfredis.zookeepercore.service.{ApacheZookeeperService, ApacheZookeeperServiceImpl}
import org.apache.zookeeper.CreateMode
import zio.{Hub, IO, Ref, ZIO, ZLayer}

case class WorkerRegistrationServiceImpl(
    service: ApacheZookeeperService,
    clusterState: Ref[ZookeeperClusterState],
    appConfig: AppConfig,
    config: ZookeeperConfig,
) extends WorkerRegistrationService {
  override def registerWorkerNode(): IO[DomainError, Unit] = {
    val data = ZookeeperNode.encodeData(appConfig.groupName, appConfig.server.internalHostName)

    clusterState.get.flatMap {
      case state if state.isLeader => ZIO.unit
      case state =>
        for {
          _                 <- ZIO.logTrace("Registering new worker node...")
          workersNodeExists <- service.exists(config.workersPath)
          _                 <- if (!workersNodeExists) service.create(config.workersPath, None, CreateMode.PERSISTENT) else ZIO.unit
          createdNode       <- service.create(s"${config.workersPath}/worker", Some(data), CreateMode.EPHEMERAL_SEQUENTIAL)
          _                 <- clusterState.update(state => state.copy(workerNode = Some(createdNode)))
          _                 <- ZIO.logTrace(s"Created new worker node '${createdNode.path}'")
        } yield ()
    }
  }
}

object WorkerRegistrationServiceImpl {
  val live: ZLayer[
    Hub[WatcherEvent] & AppConfig & Ref[ZookeeperClusterState] & ZookeeperConfig,
    DomainError,
    WorkerRegistrationService,
  ] =
    ZLayer.fromZIO {
      for {
        config       <- ZIO.service[ZookeeperConfig]
        clusterState <- ZIO.service[Ref[ZookeeperClusterState]]
        appConfig    <- ZIO.service[AppConfig]
        hub          <- ZIO.service[Hub[WatcherEvent]]
        keeper       <- ZKConnection.connect(config.host, config.port)
      } yield WorkerRegistrationServiceImpl(ApacheZookeeperServiceImpl(keeper, hub), clusterState, appConfig, config)
    }
}
