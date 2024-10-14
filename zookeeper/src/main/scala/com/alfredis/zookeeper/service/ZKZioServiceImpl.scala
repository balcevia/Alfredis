package com.alfredis.zookeeper.service
import com.alfredis.error.{DomainError, ZookeeperError}
import com.alfredis.zookeeper.model.WatcherEvent
import com.alfredis.zookeeper.model.WatcherEventType.ChildrenChange
import org.apache.zookeeper.*
import org.apache.zookeeper.KeeperException.Code
import org.apache.zookeeper.Watcher.Event.EventType
import org.apache.zookeeper.data.Stat
import zio.{Hub, URLayer, Unsafe, ZIO, ZLayer}

import scala.jdk.CollectionConverters.*

case class ZKZioServiceImpl(keeper: ZooKeeper, events: Hub[WatcherEvent]) extends ZKZioService {

  override def getChildren(path: String, watcher: Boolean): ZIO[Any, DomainError, List[String]] = {
    ZIO.async { callback =>
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
  }

  override def get(path: String): ZIO[Any, DomainError, Option[Array[Byte]]] = {
    ZIO.async { callback =>
      val dataCallback = new AsyncCallback.DataCallback {
        def processResult(rc: Int, path: String, context: Object, data: Array[Byte], stat: Stat): Unit = {
          (Code.get(rc): @unchecked) match {
            case Code.OK => callback(ZIO.succeed(Option(data)))
            case code    => callback(ZIO.fail(KeeperException.create(code)).mapError(ZookeeperError.apply))
          }
        }
      }

      keeper.getData(path, null, dataCallback, null)
    }
  }
}

object ZKZioServiceImpl {
  val live: URLayer[Hub[WatcherEvent] & ZooKeeper, ZKZioServiceImpl] = ZLayer.fromZIO {
    for {
      keeper <- ZIO.service[ZooKeeper]
      events <- ZIO.service[Hub[WatcherEvent]]
    } yield ZKZioServiceImpl(keeper, events)
  }
}
