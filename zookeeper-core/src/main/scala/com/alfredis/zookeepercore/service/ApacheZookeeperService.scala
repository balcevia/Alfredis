package com.alfredis.zookeepercore.service

import com.alfredis.error.DomainError
import com.alfredis.zookeepercore.config.ZookeeperNode
import com.alfredis.zookeepercore.model.WatcherEventType
import org.apache.zookeeper.CreateMode
import zio.{IO, ZIO}

trait ApacheZookeeperService {
  def getData(path: String): IO[DomainError, ZookeeperNode]
  def getChildren(path: String, eventType: Option[WatcherEventType]): IO[DomainError, List[String]]
  def exists(path: String): IO[DomainError, Boolean]
  def create(path: String, data: Option[String], createMode: CreateMode): IO[DomainError, ZookeeperNode]
  def getChildrenWithData(path: String, eventType: Option[WatcherEventType]): IO[DomainError, Map[String, ZookeeperNode]]
  def remove(path: String, version: Int): ZIO[Any, DomainError, Unit]
}
