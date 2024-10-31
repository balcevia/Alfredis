package com.alfredis.zookeepercore.service

import com.alfredis.error.{DomainError, ZookeeperDataDeserializationError, ZookeeperError}
import com.alfredis.zookeepercore.config.ZookeeperNode
import com.alfredis.zookeepercore.model.{WatcherEvent, WatcherEventType}
import org.apache.zookeeper.*
import org.apache.zookeeper.KeeperException.Code
import org.apache.zookeeper.Watcher.Event.EventType
import org.apache.zookeeper.data.Stat
import zio.{Hub, IO, Unsafe, ZIO}

import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.*

case class ApacheZookeeperServiceImpl(keeper: ZooKeeper, events: Hub[WatcherEvent]) extends ApacheZookeeperService {

  override def getData(path: String): IO[DomainError, ZookeeperNode] = ZIO.async { callback =>
    val dataCallback = new AsyncCallback.DataCallback:
      override def processResult(rc: Int, path: String, ctx: Any, data: Array[Byte], stat: Stat): Unit = {
        (Code.get(rc): @unchecked) match {
          case Code.OK =>
            callback(
              ZIO
                .attempt(new String(data, StandardCharsets.UTF_8))
                .map(str => ZookeeperNode(path, str, stat.getVersion))
                .mapError(ZookeeperDataDeserializationError.apply),
            )
          case code => callback(ZIO.fail(KeeperException.create(code)).mapError(ZookeeperError.apply))
        }
      }
    keeper.getData(path, null, dataCallback, null)
  }

  override def getChildren(path: String, eventType: Option[WatcherEventType]): IO[DomainError, List[String]] = ZIO.async {
    callback =>
      val dataCallback = new AsyncCallback.ChildrenCallback:
        override def processResult(rc: Int, path: String, ctx: Any, children: java.util.List[String]): Unit = {
          (Code.get(rc): @unchecked) match {
            case Code.OK => callback(ZIO.succeed(children.asScala.toList))
            case code    => callback(ZIO.fail(KeeperException.create(code)).mapError(ZookeeperError.apply))
          }
        }

      val childrenWatcher: Option[Watcher] = eventType.map { e =>
        (event: WatchedEvent) => {
          event.getType match {
            case EventType.NodeChildrenChanged =>
              Unsafe.unsafe(implicit unsafe =>
                zio.Runtime.default.unsafe.run(events.offer(WatcherEvent(e, event.getPath))).getOrThrow(),
              )
              ()
            case _ => ()
          }
        }
      }

      keeper.getChildren(path, childrenWatcher.orNull, dataCallback, null)
  }

  override def exists(path: String): IO[DomainError, Boolean] = ZIO.async { callback =>
    val dataCallback = new AsyncCallback.StatCallback:
      override def processResult(rc: Int, path: String, ctx: Any, stat: Stat): Unit = {
        (Code.get(rc): @unchecked) match {
          case Code.OK     => callback(ZIO.succeed(stat != null))
          case Code.NONODE => callback(ZIO.succeed(false))
          case code        => callback(ZIO.fail(KeeperException.create(code)).mapError(ZookeeperError.apply))
        }
      }

    keeper.exists(path, null, dataCallback, null)
  }

  override def create(path: String, data: Option[String], createMode: CreateMode): IO[DomainError, ZookeeperNode] = ZIO.async { callback =>
    val dataCallback = new AsyncCallback.Create2Callback:
      override def processResult(rc: Int, path: String, ctx: Any, name: String, stat: Stat): Unit = {
        (Code.get(rc): @unchecked) match {
          case Code.OK => callback(ZIO.succeed(ZookeeperNode(name, data.orNull, stat.getVersion)))
          case code    => callback(ZIO.fail(KeeperException.create(code)).mapError(ZookeeperError.apply))
        }
      }

    keeper.create(path, data.map(_.getBytes).orNull, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode, dataCallback, null)
  }

  override def getChildrenWithData(path: String, eventType: Option[WatcherEventType]): IO[DomainError, Map[String, ZookeeperNode]] = {
    getChildren(path, eventType).flatMap { children =>
      ZIO
        .collectAll {
          children.map(child => getData(s"$path/$child").map(node => child -> node))
        }
        .map(_.toMap)
    }
  }

  override def remove(path: String, version: Int): ZIO[Any, DomainError, Unit] = ZIO.async { callback =>
    val dataCallback = new AsyncCallback.VoidCallback {
      override def processResult(rc: Int, path: String, ctx: Any): Unit = {
        (Code.get(rc): @unchecked) match {
          case Code.OK => callback(ZIO.succeed(()))
          case code    => callback(ZIO.fail(KeeperException.create(code)).mapError(ZookeeperError.apply))
        }
      }
    }
    keeper.delete(path, version, dataCallback, null)
  }
}
