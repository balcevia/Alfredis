package com.alfredis.zookeeper.curator

import com.alfredis.error.{DomainError, ZookeeperDataDeserializationError, ZookeeperError}
import com.alfredis.zookeeper.config.ZookeeperNode
import com.alfredis.zookeeper.model.WatcherEvent
import com.alfredis.zookeeper.model.WatcherEventType.ChildrenChange
import org.apache.zookeeper.*
import org.apache.zookeeper.KeeperException.Code
import org.apache.zookeeper.Watcher.Event.EventType
import org.apache.zookeeper.data.Stat
import zio.{Hub, Unsafe, ZIO}

import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*

case class ApacheZookeeperServiceImpl(keeper: ZooKeeper, events: Hub[WatcherEvent]) {
  
  def getDataAsString(path: String): ZIO[Any, DomainError, String] = ZIO.async { callback =>
    val dataCallback = new AsyncCallback.DataCallback:
      override def processResult(rc: Int, path: String, ctx: Any, data: Array[Byte], stat: Stat): Unit = {
        (Code.get(rc): @unchecked) match {
          case Code.OK => callback(ZIO.attempt(new String(data, StandardCharsets.UTF_8)).mapError(ZookeeperDataDeserializationError.apply))
          case code    => callback(ZIO.fail(KeeperException.create(code)).mapError(ZookeeperError.apply))
        }
      }
    keeper.getData(path, null, dataCallback, null)
  }

  def getChildren(path: String, watcher: Boolean): ZIO[Any, DomainError, List[String]] = ZIO.async { callback =>
    val dataCallback = new AsyncCallback.ChildrenCallback:
      override def processResult(rc: Int, path: String, ctx: Any, children: java.util.List[String]): Unit = {
        (Code.get(rc): @unchecked) match {
          case Code.OK => callback(ZIO.succeed(children.asScala.toList))
          case code    => callback(ZIO.fail(KeeperException.create(code)).mapError(ZookeeperError.apply))
        }
      }

    val childrenWatcher: Option[Watcher] = Option.when(watcher) { (event: WatchedEvent) =>
      {
        event.getType match {
          case EventType.NodeChildrenChanged =>
            Unsafe.unsafe(implicit unsafe =>
              zio.Runtime.default.unsafe.run(events.offer(WatcherEvent(ChildrenChange, event.getPath))).getOrThrow(),
            )
            ()
          case _ => ()
        }
      }
    }

    keeper.getChildren(path, childrenWatcher.orNull, dataCallback, null)
  }

  def exists(path: String): ZIO[Any, DomainError, Boolean] = ZIO.async { callback =>
    val dataCallback = new AsyncCallback.StatCallback:
      override def processResult(rc: Int, path: String, ctx: Any, stat: Stat): Unit = {
        (Code.get(rc): @unchecked) match {
          case Code.OK => callback(ZIO.succeed(stat != null))
          case code    => callback(ZIO.fail(KeeperException.create(code)).mapError(ZookeeperError.apply))
        }
      }

    keeper.exists(path, null, dataCallback, null)
  }

  def create(path: String, createMode: CreateMode): ZIO[Any, DomainError, Unit] = ZIO.async { callback =>
    val dataCallback = new AsyncCallback.Create2Callback:
      override def processResult(rc: Int, path: String, ctx: Any, name: String, stat: Stat): Unit = {
        (Code.get(rc): @unchecked) match {
          case Code.OK => callback(ZIO.succeed(()))
          case code    => callback(ZIO.fail(KeeperException.create(code)).mapError(ZookeeperError.apply))
        }
      }

    keeper.create(path, null, null, createMode, dataCallback, null)
  }

  def getChildrenWithData(path: String): ZIO[Any, DomainError, Map[String, ZookeeperNode]] = {
    getChildren(path, true).flatMap { children =>
      ZIO
        .collectAll {
          children.map(child => getDataAsString(s"$path/$child").map(data => child -> ZookeeperNode(s"$path/$child", data)))
        }
        .map(_.toMap)
    }
  }
}
