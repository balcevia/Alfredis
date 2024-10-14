package com.alfredis.zookeeper.curator

import com.alfredis.error.DomainError
import com.alfredis.zookeeper.config.{AppConfig, ZookeeperClusterState, ZookeeperConfig}
import com.alfredis.zookeeper.model.WatcherEvent
import com.alfredis.zookeeper.service.ZKConnection
import org.apache.zookeeper.CreateMode
import zio.{Hub, Ref, ZIO, ZLayer}

case class StartUpServiceImpl(
    zookeeperService: ZookeeperService,
    leadersWatcher: LeadersWatcher,
    clusterState: Ref[ZookeeperClusterState],
    appConfig: AppConfig,
) extends StartUpService {

  override def startup(): ZIO[Any, Nothing, Unit] = for {
    leaderExists <- zookeeperService.exists(appConfig.leadersPath)
    _            <- if (leaderExists) ZIO.unit else zookeeperService.create(appConfig.leadersPath, CreateMode.PERSISTENT)
    _            <- if (leaderExists) initializeClusterState() else ZIO.unit
  } yield ()

  private def initializeClusterState(): ZIO[Any, Nothing, Unit] = for {
    _       <- ZIO.logInfo("Initialising cluster state...")
    leaders <- zookeeperService.getChildrenWithData(appConfig.leadersPath)
    _       <- ZIO.logInfo(s"Received leaders: ${leaders.mkString(", ")}")
    _       <- clusterState.update(state => state.copy(leaders = leaders))
    _       <- clusterState.get.flatMap(state => ZIO.logInfo(s"Cluster state: $state"))
    _ = leadersWatcher.watchChildren()
  } yield ()

}

object StartUpServiceImpl {
  val live: ZLayer[
    Hub[WatcherEvent] & AppConfig & Ref[ZookeeperClusterState] & LeadersWatcher & ZookeeperConfig,
    DomainError,
    StartUpServiceImpl,
  ] =
    ZLayer.fromZIO {
      for {
        config         <- ZIO.service[ZookeeperConfig]
//        asyncClient    <- ZookeeperConnection.create(config)
        leadersWatcher <- ZIO.service[LeadersWatcher]
        clusterState   <- ZIO.service[Ref[ZookeeperClusterState]]
        appConfig      <- ZIO.service[AppConfig]
        hub            <- ZIO.service[Hub[WatcherEvent]]
        keeper         <- ZKConnection.connect(config.host, config.port)
      } yield StartUpServiceImpl(ApacheZookeeperService(ApacheZookeeperServiceImpl(keeper, hub)), leadersWatcher, clusterState, appConfig)
    }
}
