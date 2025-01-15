package com.alfredis.cache.client

import com.alfredis.error.DomainError
import com.alfredis.tcp.ZIOTCPClient
import com.alfredis.zookeepercore.ZKConnection
import com.alfredis.zookeepercore.config.ZookeeperConfig
import com.alfredis.zookeepercore.model.WatcherEvent
import com.alfredis.zookeepercore.service.{ApacheZookeeperService, ApacheZookeeperServiceImpl}
import zio.{Hub, Ref, Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

case class CacheClient private (
    tcpClient: ZIOTCPClient,
    zookeeperService: ApacheZookeeperService,
    leaders: Ref[Map[String, String]],
    config: ZookeeperConfig,
    hashing: ConsistentHashing,
) {

  private def retrieveHostAndPort(nodeName: String): ZIO[Any, Nothing, (String, Int)] = {
    leaders.get.map(l => l(nodeName)).map { hostAndPort =>
      val arr = hostAndPort.split(":")
      arr.head -> arr(1).toInt
    }
  }

  def put(key: String, value: Array[Byte]): ZIO[Any, Throwable, Unit] = {
    val nodeName = hashing.getNode(key)

    retrieveHostAndPort(nodeName)
      .flatMap { // fixme if throwable is thrown retry
        case (host, port) => tcpClient.put(host, port, key, value)
      }
      .map(_ => ())
  }

  def get(key: String): ZIO[Any, Throwable, Option[Array[Byte]]] = {
    val nodeName = hashing.getNode(key)

    retrieveHostAndPort(nodeName).flatMap { // fixme if throwable is thrown retry
      case (host, port) => tcpClient.get(host, port, key)
    }
  }

//  private def updateLeadersList() = { fixme uncomment and use
//    zookeeperService.getChildrenWithData(config.leadersPath, None).flatMap { leaderNodes =>
//      val map = leaderNodes.values.map(_.decodedData).toMap
//      leaders.set(map)
//    }
//  }

}

object CacheClient {
  def create(config: ZookeeperConfig, tcpClient: ZIOTCPClient): ZIO[Any, DomainError, CacheClient] = {
    for {
      hub        <- Hub.unbounded[WatcherEvent]()
      connection <- ZKConnection.connect(config.host, config.port)
      zookeeperService = ApacheZookeeperServiceImpl(connection, hub)
      leaderNodes <- zookeeperService.getChildrenWithData(config.leadersPath, None)
      leaders = leaderNodes.values.map(_.decodedData).toMap
      leadersRef <- Ref.make(leaders)
    } yield CacheClient(tcpClient, zookeeperService, leadersRef, config, ConsistentHashing(leaders.keys.toList))
  }

  val live: ZLayer[ZookeeperConfig & ZIOTCPClient, DomainError, CacheClient] = ZLayer.fromZIO {
    for {
      config    <- ZIO.service[ZookeeperConfig]
      tcpClient <- ZIO.service[ZIOTCPClient]
      client    <- create(config, tcpClient)
    } yield client
  }
}


object XXX extends ZIOAppDefault {
  override def run: ZIO[Any & ZIOAppArgs & Scope, Any, Any] = {
    val program = for {
      client <- ZIO.service[CacheClient]
      _ <- client.put("test1", "ALAMAKOTA".getBytes)
      str <- client.get("test1").map(_.map(bytes => new String(bytes)))
      _ = println(str)
    } yield ()

    program.provide(CacheClient.live, ZookeeperConfig.live, ZIOTCPClient.live)
  }
}