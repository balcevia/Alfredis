package com.alfredis.zookeeper.curator

import com.alfredis.zookeeper.model.{WatcherEvent, WatcherEventType}
import org.apache.curator.x.async.AsyncCuratorFramework
import zio.{Hub, Unsafe}

trait ChildrenWatcher {
  protected val asyncClient: AsyncCuratorFramework
  protected val eventHub: Hub[WatcherEvent]
  protected val path: String

  def watchChildren(): Unit = {
    val _ = asyncClient.watched().getChildren.forPath(path).event().thenAccept { event =>
      Unsafe.unsafe(implicit unsafe => zio.Runtime.default.unsafe.run(eventHub.offer(WatcherEvent(WatcherEventType.ChildrenChange, path))))
      watchChildren()
    }
  }
}
