package com.alfredis.zookeeper.curator

import com.alfredis.zookeeper.config.ZookeeperNode
import org.apache.curator.x.async.AsyncCuratorFramework
import org.apache.zookeeper.CreateMode
import zio.ZIO

import scala.jdk.CollectionConverters.*

case class ZookeeperServiceImpl(asyncClient: AsyncCuratorFramework)  {
  def getDataAsString(path: String): ZIO[Any, Nothing, String] = ZIO.async { callback =>
    val _ = asyncClient.getData.forPath(path).thenAcceptAsync { data =>
      val str = new String(data)
      callback(ZIO.succeed(str))
    }
  }

  def getChildren(path: String): ZIO[Any, Nothing, List[String]] = ZIO.async { callback =>
    val _ = asyncClient.getChildren.forPath(path).thenAcceptAsync(res => callback(ZIO.succeed(res.asScala.toList)))
  }

  def exists(path: String): ZIO[Any, Nothing, Boolean] = ZIO.async { callback =>
    val _ = asyncClient.checkExists().forPath(path).thenAcceptAsync(res => callback(ZIO.succeed(res != null)))
  }

  def create(path: String, createMode: CreateMode): ZIO[Any, Nothing, Unit] = ZIO.async { callback =>
    val _ = asyncClient.create.withMode(createMode).forPath(path).thenAcceptAsync(_ => callback(ZIO.unit))
  }

  def getChildrenWithData(path: String): ZIO[Any, Nothing, Map[String, ZookeeperNode]] = {
    getChildren(path).flatMap { children =>
      ZIO
        .collectAll {
          children.map { child =>
            getDataAsString(s"$path/$child")
              .map(data => ZookeeperNode(s"$path/$child", data, 0)) //fixme version if needed
              .map(node => node.path -> node)
          }
        }
        .map(_.toMap)
    }
  }
}
