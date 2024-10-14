package com.alfredis.zookeeper.curator

import com.alfredis.zookeeper.config.ZookeeperNode
import org.apache.zookeeper.CreateMode
import zio.ZIO

case class ApacheZookeeperService(service: ApacheZookeeperServiceImpl) extends ZookeeperService {
  override def getDataAsString(path: String): ZIO[Any, Nothing, String] = service
    .getDataAsString(path)
    .foldZIO(
      ex => ZIO.logError(s"getDataAsString failed with exception: ${ex.message}") *> ZIO.succeed(""),
      data => ZIO.succeed(data),
    )

  override def getChildren(path: String): ZIO[Any, Nothing, List[String]] = service
    .getChildren(path, true)
    .foldZIO(
      ex => ZIO.logError(s"getChildren failed with exception: ${ex.message}") *> ZIO.succeed(List.empty),
      data => ZIO.succeed(data),
    )

  override def exists(path: String): ZIO[Any, Nothing, Boolean] = service
    .exists(path)
    .foldZIO(
      ex => ZIO.logError(s"exists failed with exception: ${ex.message}") *> ZIO.succeed(false),
      data => ZIO.succeed(data),
    )

  override def create(path: String, createMode: CreateMode): ZIO[Any, Nothing, Unit] = service
    .create(path, createMode)
    .foldZIO(
      ex => ZIO.logError(s"create failed with exception: ${ex.message}") *> ZIO.succeed(()),
      data => ZIO.succeed(data),
    )

  override def getChildrenWithData(path: String): ZIO[Any, Nothing, Map[String, ZookeeperNode]] = service
    .getChildrenWithData(path)
    .foldZIO(
      ex => ZIO.logError(s"getChildrenWithData failed with exception: ${ex.message}") *> ZIO.succeed(Map.empty),
      data => ZIO.succeed(data),
    )
}
