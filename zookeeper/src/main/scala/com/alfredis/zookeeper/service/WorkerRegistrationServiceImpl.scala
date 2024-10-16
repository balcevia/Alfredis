package com.alfredis.zookeeper.service

import com.alfredis.error.DomainError
import com.alfredis.zookeeper.config.{AppConfig, ZookeeperClusterState, ZookeeperConfig}
import com.alfredis.zookeeper.model.WatcherEvent
import org.apache.zookeeper.CreateMode
import zio.{Hub, IO, Ref, ZIO, ZLayer}

case class WorkerRegistrationServiceImpl(service: ApacheZookeeperService, clusterState: Ref[ZookeeperClusterState], appConfig: AppConfig)
    extends WorkerRegistrationService {
  override def registerWorkerNode(): IO[DomainError, Unit] = {
    val data = "Workers test data" // fixme add actual data here

    clusterState.get.flatMap {
      case state if state.isLeader => ZIO.unit
      case state =>
        for {
          workersNodeExists <- service.exists(appConfig.workersPath)
          _                 <- if (!workersNodeExists) service.create(appConfig.workersPath, None, CreateMode.PERSISTENT) else ZIO.unit
          createdNode       <- service.create(s"${appConfig.workersPath}/worker", Some(data), CreateMode.EPHEMERAL_SEQUENTIAL)
          _                 <- clusterState.update(state => state.copy(workerNode = Some(createdNode)))
          _                 <- ZIO.logInfo(s"Created new worker node '${createdNode.path}'")
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
      } yield WorkerRegistrationServiceImpl(ApacheZookeeperServiceImpl(keeper, hub), clusterState, appConfig)
    }
}
