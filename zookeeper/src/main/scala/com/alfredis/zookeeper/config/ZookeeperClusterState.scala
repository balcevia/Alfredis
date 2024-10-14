package com.alfredis.zookeeper.config

import zio.{Ref, ULayer, ZLayer}

case class ZookeeperClusterState(leaders: Map[String, ZookeeperNode])

object ZookeeperClusterState {
  val live: ULayer[Ref[ZookeeperClusterState]] = ZLayer.fromZIO {
    Ref.make(ZookeeperClusterState(Map.empty))
  }
}

case class ZookeeperNode(path: String, data: String)
