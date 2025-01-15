package com.alfredis.cache.server.http.service

import com.alfredis.api.model.CacheEntryRequest
import com.alfredis.cache.{Cache, CacheRecord}
import com.alfredis.tcp.ZIOTCPClient
import com.alfredis.zookeepercore.config.ZookeeperClusterState
import zio.{Ref, UIO, ZIO, ZLayer}

case class CacheServiceImpl(cache: Ref[Cache[String, Array[Byte]]], clusterState: Ref[ZookeeperClusterState], tcpClient: ZIOTCPClient)
    extends CacheService {
  override def put(entries: Seq[CacheEntryRequest], isLeader: Boolean): UIO[Unit] = {
    for {
      _ <- cache.get.map(c => entries.foreach(e => c.put(e.key, e.value)))
      _ <- if (isLeader) replicateToWorkers(entries) else ZIO.unit
    } yield ()
  }

  private def replicateToWorkers(entries: Seq[CacheEntryRequest]): UIO[Unit] = {
    val result = for {
      state <- clusterState.get
      hostsAndPorts = state.workers.map(w => w.getHostAndPort)
      _ <- ZIO.collectAllPar(hostsAndPorts.map { case (host, port) =>
        tcpClient.put(host, port, entries.toList, state.currentLeader.map(_.path))
      })
    } yield ()

    result.foldZIO(
      error => ZIO.logError(s"Replication failed: ${error.getMessage}") *> ZIO.unit,
      _ => ZIO.logTrace(s"Replication succeeded") *> ZIO.unit,
    )
  }

  override def get(key: String): UIO[Option[Array[Byte]]] = cache.get.map(_.get(key))

  override def getAll: UIO[List[CacheRecord[String, Array[Byte]]]] = cache.get.map(_.getAll)

  override def retrieveStateFromLeaderIfNeeded(): UIO[Unit] = {
    val result = clusterState.get.flatMap { state =>
      if (!state.isLeader && state.currentLeader.isDefined) {
        val (host, port) = state.currentLeader.get.getHostAndPort
        for {
          response <- tcpClient.getAll(host, port, state.workerNode.map(_.path))
          _        <- ZIO.logTrace(s"Received data from the leader, data size: ${response.size}")
          _ <- cache.update { c =>
            response.foreach(e => c.put(e._1, e._2)); c
          }
        } yield ()
      } else ZIO.logTrace("Current node is a leader, data sync is not needed") *> ZIO.unit
    }

    result.foldZIO(
      error => ZIO.logError(error.getMessage),
      v => ZIO.succeed(v),
    )
  }
}

object CacheServiceImpl {
  val live: ZLayer[ZIOTCPClient & Ref[ZookeeperClusterState] & Ref[Cache[String, Array[Byte]]], Nothing, CacheServiceImpl] =
    ZLayer.fromZIO {
      for {
        cache        <- ZIO.service[Ref[Cache[String, Array[Byte]]]]
        clusterState <- ZIO.service[Ref[ZookeeperClusterState]]
        tcpClient    <- ZIO.service[ZIOTCPClient]
      } yield CacheServiceImpl(cache, clusterState, tcpClient)
    }
}
