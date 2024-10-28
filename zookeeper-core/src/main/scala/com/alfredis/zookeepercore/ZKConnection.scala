package com.alfredis.zookeepercore

import com.alfredis.error.ZookeeperConnectionError
import org.apache.zookeeper.*
import org.apache.zookeeper.Watcher.Event.KeeperState
import zio.{IO, Unsafe, ZIO}

import java.util
import java.util.concurrent.CountDownLatch
import scala.jdk.CollectionConverters.*

object ZKConnection {
  def connect(host: String, port: Int): IO[ZookeeperConnectionError, ZooKeeper] =
    ZIO
      .attemptBlocking(blockingConnect(host, port))
      .mapError(e => ZookeeperConnectionError(e.getMessage))

  def blockingConnect(host: String, port: Int): ZooKeeper = {
    val connectedSignal: CountDownLatch = new CountDownLatch(1)

    val zoo = new ZooKeeper(
      host,
      port,
      (event: WatchedEvent) => {
        if (event.getState == KeeperState.SyncConnected) {
          connectedSignal.countDown()
        }
      },
    )

    connectedSignal.await()
    zoo
  }

}
