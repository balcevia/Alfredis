package com.alfredis.zookeeper.service

import com.alfredis.error.DomainError
import com.alfredis.zookeeper.config.{AppConfig, ZookeeperClusterState, ZookeeperConfig}
import com.alfredis.zookeeper.model.WatcherEvent
import com.alfredis.zookeeper.model.WatcherEventType.ChildrenChange
import com.alfredis.zookeeper.service
import org.apache.zookeeper.CreateMode
import zio.{Hub, Ref, ZIO, ZLayer}

case class StartUpServiceImpl(
    zookeeperService: ApacheZookeeperService,
    clusterState: Ref[ZookeeperClusterState],
    appConfig: AppConfig,
) extends StartUpService {

  override def startup(): ZIO[Any, DomainError, Unit] = for {
    leaderExists <- zookeeperService.exists(appConfig.leadersPath)
    _            <- if (leaderExists) ZIO.unit else zookeeperService.create(appConfig.leadersPath, None, CreateMode.PERSISTENT)
    _            <- if (leaderExists) initializeClusterState() else ZIO.unit
  } yield ()

  private def initializeClusterState(): ZIO[Any, DomainError, Unit] = for {
    _       <- ZIO.logInfo("Initialising cluster state...")
    leaders <- zookeeperService.getChildrenWithData(appConfig.leadersPath, ChildrenChange)
    _       <- ZIO.logInfo(s"Received leaders: ${leaders.mkString(", ")}")
    _       <- clusterState.update(state => state.copy(leaders = leaders))
    _       <- clusterState.get.flatMap(state => ZIO.logInfo(s"Cluster state: $state"))
  } yield ()

}

object StartUpServiceImpl {
  val live: ZLayer[
    Hub[WatcherEvent] & AppConfig & Ref[ZookeeperClusterState] & ZookeeperConfig,
    DomainError,
    StartUpServiceImpl,
  ] =
    ZLayer.fromZIO {
      for {
        config         <- ZIO.service[ZookeeperConfig]
        clusterState   <- ZIO.service[Ref[ZookeeperClusterState]]
        appConfig      <- ZIO.service[AppConfig]
        hub            <- ZIO.service[Hub[WatcherEvent]]
        keeper         <- ZKConnection.connect(config.host, config.port)
      } yield service.StartUpServiceImpl(ApacheZookeeperServiceImpl(keeper, hub), clusterState, appConfig)
    }
}
