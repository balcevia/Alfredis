package com.alfredis.cache.client

import com.alfredis.error.DomainError
import com.alfredis.tcp.ZIOTCPClient
import com.alfredis.zookeepercore.ZKConnection
import com.alfredis.zookeepercore.config.ZookeeperConfig
import com.alfredis.zookeepercore.model.WatcherEvent
import com.alfredis.zookeepercore.service.{ApacheZookeeperService, ApacheZookeeperServiceImpl}
import redis.clients.jedis.JedisPool
import zio.{Hub, Ref, Scope, ULayer, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

import java.util.UUID

trait CacheClient {
  def put(key: String, value: Array[Byte]): ZIO[Any, Throwable, Unit]
  def get(key: String): ZIO[Any, Throwable, Option[Array[Byte]]]
}

case class RedisCacheClient() extends CacheClient {
  private val pool  = new JedisPool("localhost", 6379)
  private val jedis = pool.getResource

  override def put(key: String, value: Array[Byte]): ZIO[Any, Throwable, Unit] =
    ZIO.attempt(jedis.set("test", "First test")).unit

  override def get(key: String): ZIO[Any, Throwable, Option[Array[Byte]]] =
    ZIO.attempt(jedis.get("test")).map(v => Option(v).map(_.getBytes))
}

object RedisCacheClient {
  val live: ULayer[RedisCacheClient] = ZLayer.succeed(RedisCacheClient())
}

case class AlfredisCacheClientImpl private (
    tcpClient: ZIOTCPClient,
    zookeeperService: ApacheZookeeperService,
    leaders: Ref[Map[String, String]],
    config: ZookeeperConfig,
    hashing: ConsistentHashing,
) extends CacheClient {

  private def retrieveHostAndPort(nodeName: String): ZIO[Any, Nothing, (String, Int)] = {
    leaders.get.map(l => l(nodeName)).map { hostAndPort =>
      val arr = hostAndPort.split(":")
      arr.head -> arr(1).toInt
    }
  }

  override def put(key: String, value: Array[Byte]): ZIO[Any, Throwable, Unit] = {
    val nodeName = hashing.getNode(key)

    retrieveHostAndPort(nodeName)
      .flatMap { // fixme if throwable is thrown retry
        case (host, port) =>
          println(s"$host: $port") // todo remove
          tcpClient.put(host, port, key, value)
      }
      .map(_ => ())
  }

  override def get(key: String): ZIO[Any, Throwable, Option[Array[Byte]]] = {
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

object AlfredisCacheClientImpl {
  def create(config: ZookeeperConfig, tcpClient: ZIOTCPClient): ZIO[Any, DomainError, AlfredisCacheClientImpl] = {
    for {
      hub        <- Hub.unbounded[WatcherEvent]()
      connection <- ZKConnection.connect(config.host, config.port)
      zookeeperService = ApacheZookeeperServiceImpl(connection, hub)
      leaderNodes <- zookeeperService.getChildrenWithData(config.leadersPath, None)
      leaders = leaderNodes.values.map(_.decodedData).toMap
      leadersRef <- Ref.make(leaders)
    } yield AlfredisCacheClientImpl(tcpClient, zookeeperService, leadersRef, config, ConsistentHashing(leaders.keys.toList))
  }

  val live: ZLayer[ZookeeperConfig & ZIOTCPClient, DomainError, AlfredisCacheClientImpl] = ZLayer.fromZIO {
    for {
      config    <- ZIO.service[ZookeeperConfig]
      tcpClient <- ZIO.service[ZIOTCPClient]
      client    <- create(config, tcpClient)
    } yield client
  }
}

object ExampleApp extends ZIOAppDefault {
  override def run: ZIO[Any & ZIOAppArgs & Scope, Any, Any] = {
//    val program = for {
//      client <- ZIO.service[CacheClient]
//      _ <- client.put("test1", "ALAMAKOTA".getBytes)
//      _ <- client.put(UUID.randomUUID().toString, "ascasmxaksnxaj".getBytes)
//      _ <- client.put(UUID.randomUUID().toString, "admaskd".getBytes)
//      str <- client.get("test1").map(_.map(bytes => new String(bytes)))
//      _ = println(str)
//    } yield ()

//    program.provide(CacheClient.live, ZookeeperConfig.live, ZIOTCPClient.live)

    val test = for {
      client <- ZIO.service[AlfredisCacheClientImpl]
      _      <- ZIO.collectAll((1 to 100).map(_ => client.put(UUID.randomUUID().toString, UUID.randomUUID().toString.getBytes())))
    } yield ()

    test.provide(AlfredisCacheClientImpl.live, ZookeeperConfig.live, ZIOTCPClient.live)
  }
}
