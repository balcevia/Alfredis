package com.alfredis.zookeeper.service

import com.alfredis.error.DomainError
import com.alfredis.zookeeper.config.ZookeeperNode
import org.apache.zookeeper.CreateMode
import zio.ZIO

trait ApacheZookeeperService {
  def getDataAsString(path: String): ZIO[Any, DomainError, String]
  def getChildren(path: String, watcher: Boolean): ZIO[Any, DomainError, List[String]]
  def exists(path: String): ZIO[Any, DomainError, Boolean]
  def create(path: String, createMode: CreateMode): ZIO[Any, DomainError, Unit]
  def getChildrenWithData(path: String): ZIO[Any, DomainError, Map[String, ZookeeperNode]]
}
