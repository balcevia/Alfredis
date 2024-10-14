package com.alfredis.zookeeper.curator

import com.alfredis.zookeeper.config.ZookeeperNode
import org.apache.zookeeper.CreateMode
import zio.ZIO

trait ZookeeperService {
  def getDataAsString(path: String): ZIO[Any, Nothing, String]
  def getChildren(path: String): ZIO[Any, Nothing, List[String]]
  def exists(path: String): ZIO[Any, Nothing, Boolean]
  def create(path: String, createMode: CreateMode): ZIO[Any, Nothing, Unit]
  def getChildrenWithData(path: String): ZIO[Any, Nothing, Map[String, ZookeeperNode]]
}
