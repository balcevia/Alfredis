package com.alfredis.zookeepercore.config

import zio.{Ref, ULayer, ZLayer}

case class ZookeeperClusterState(
    electionNode: Option[ZookeeperNode],
    isLeader: Boolean,
    currentLeader: Option[ZookeeperNode],
    workerNode: Option[ZookeeperNode],
    workers: List[ZookeeperNode],
)

object ZookeeperClusterState {
  private val empty: ZookeeperClusterState = ZookeeperClusterState(
    electionNode = None,
    isLeader = false,
    currentLeader = None,
    workerNode = None,
    workers = List.empty
  )
  val live: ULayer[Ref[ZookeeperClusterState]] = ZLayer.fromZIO {
    Ref.make(empty)
  }
}

case class ZookeeperNode(path: String, data: String, version: Int)
