package com.alfredis.zookeeper.config

import zio.{Ref, ULayer, ZLayer}

case class ZookeeperClusterState(
    leaders: Map[String, ZookeeperNode],
    electionNode: Option[ZookeeperNode],
    isLeader: Boolean,
    currentLeader: Option[String],
    workerNode: Option[ZookeeperNode],
)

object ZookeeperClusterState {
  private val empty: ZookeeperClusterState = ZookeeperClusterState(
    leaders = Map.empty,
    electionNode = None,
    isLeader = false,
    currentLeader = None,
    workerNode = None,
  )
  val live: ULayer[Ref[ZookeeperClusterState]] = ZLayer.fromZIO {
    Ref.make(empty)
  }
}

case class ZookeeperNode(path: String, data: String, version: Int)
