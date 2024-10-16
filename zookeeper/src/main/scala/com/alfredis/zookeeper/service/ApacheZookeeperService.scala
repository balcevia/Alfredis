package com.alfredis.zookeeper.service

import com.alfredis.error.DomainError
import com.alfredis.zookeeper.config.ZookeeperNode
import com.alfredis.zookeeper.model.WatcherEventType
import org.apache.zookeeper.CreateMode
import zio.{IO, ZIO}

trait ApacheZookeeperService {
  def getDataAsString(path: String): IO[DomainError, ZookeeperNode]
  def getChildren(path: String, watcher: Boolean, eventType: WatcherEventType): IO[DomainError, List[String]]
  def exists(path: String): IO[DomainError, Boolean]
  def create(path: String, data: Option[String], createMode: CreateMode): IO[DomainError, ZookeeperNode]
  def getChildrenWithData(path: String, eventType: WatcherEventType): IO[DomainError, Map[String, ZookeeperNode]]
  def remove(path: String, version: Int): ZIO[Any, DomainError, Unit]
}
