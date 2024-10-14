package com.alfredis.zookeeper.curator

import com.alfredis.error.DomainError
import com.alfredis.zookeeper.config.{AppConfig, ZookeeperConfig}
import com.alfredis.zookeeper.model.WatcherEvent
import org.apache.curator.x.async.AsyncCuratorFramework
import zio.{Hub, ZIO, ZLayer}

case class LeadersWatcher(
    override protected val asyncClient: AsyncCuratorFramework,
    override protected val eventHub: Hub[WatcherEvent],
    override protected val path: String,
) extends ChildrenWatcher {}

object LeadersWatcher {
  val live: ZLayer[Hub[WatcherEvent] & AppConfig & ZookeeperConfig, DomainError, LeadersWatcher] = ZLayer.fromZIO {
    for {
      zookeeperConfig <- ZIO.service[ZookeeperConfig]
      appConfig       <- ZIO.service[AppConfig]
      asyncClient     <- ZookeeperConnection.create(zookeeperConfig)
      eventHub        <- ZIO.service[Hub[WatcherEvent]]
    } yield LeadersWatcher(asyncClient, eventHub, appConfig.leadersPath)
  }
}
