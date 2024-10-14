package com.alfredis.zookeeper.curator

import com.alfredis.error.{DomainError, ZookeeperError}
import com.alfredis.zookeeper.config.ZookeeperConfig
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.RetryNTimes
import org.apache.curator.x.async.AsyncCuratorFramework
import zio.{IO, ZIO}

object ZookeeperConnection {
  def create(config: ZookeeperConfig): IO[DomainError, AsyncCuratorFramework] = ZIO
    .attemptBlocking {
      val retryPolicy = new RetryNTimes(config.maxRetries, config.sleepMsBetweenRetries)
      val client      = CuratorFrameworkFactory.newClient(s"${config.host}:${config.port}", retryPolicy)
      client.start()
      AsyncCuratorFramework.wrap(client)
    }
    .mapError(ZookeeperError.apply)
}
