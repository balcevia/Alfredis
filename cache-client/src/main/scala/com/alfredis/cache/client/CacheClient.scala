package com.alfredis.cache.client

import com.alfredis.api.model.{CacheEntryRequest, CreateEntryRequest}
import com.alfredis.error.{DomainError, HttpClientError, HttpClientSendingRequestError}
import com.alfredis.httpclient.HttpClient
import com.alfredis.zookeepercore.ZKConnection
import com.alfredis.zookeepercore.config.ZookeeperConfig
import com.alfredis.zookeepercore.model.WatcherEvent
import com.alfredis.zookeepercore.service.{ApacheZookeeperService, ApacheZookeeperServiceImpl}
import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.{Hub, IO, Ref, UIO, ZIO, ZLayer}

case class CacheClient private (
    httpClientPool: HttpClientPool,
    zookeeperService: ApacheZookeeperService,
    leaders: Ref[Map[String, String]],
    config: ZookeeperConfig,
    hashing: ConsistentHashing,
) {

  private def createEntryUrl(host: String): String           = s"http://$host/cache/create"
  private def getEntryUrl(host: String, key: String): String = s"http://$host/cache/$key"

  private def sendRequest[T](nodeName: String, callApi: String => IO[DomainError, T], onSuccess: T => UIO[T], defaultValue: T): UIO[T] = {

    def sendRequest(): IO[DomainError, T] = {
      for {
        host     <- leaders.get.map(l => l(nodeName))
        response <- callApi(host)
      } yield response
    }

    def onError(error: DomainError) = ZIO.logError(error.message) *> ZIO.succeed(defaultValue)

    sendRequest().foldZIO(
      {
        case HttpClientSendingRequestError(message) =>
          val result = for {
            _        <- ZIO.logInfo("Retrying request to the cache...")
            _        <- updateLeadersList()
            response <- sendRequest()
          } yield response

          result.foldZIO(
            onError,
            onSuccess,
          )

        case error => onError(error)
      },
      onSuccess,
    )
  }

  def put(key: String, value: Array[Byte]): ZIO[Any, Nothing, Unit] = {
    val client = httpClientPool.next
    val nodeName = hashing.getNode(key)
    val entry    = CreateEntryRequest(None, Seq(CacheEntryRequest(key, value)))

    def callApi(host: String) = client.callApiUnit(client.postRequest(createEntryUrl(host), entry))

    sendRequest[Unit](nodeName, callApi, _ => ZIO.unit, ())
  }

  def get(key: String): ZIO[Any, Nothing, Option[Array[Byte]]] = {
    val client = httpClientPool.next
    val nodeName = hashing.getNode(key)

    def callApi(host: String) = client.callApi[Option[Array[Byte]]](client.getRequest(getEntryUrl(host, key)))

    sendRequest[Option[Array[Byte]]](nodeName, callApi, data => ZIO.succeed(data), None)
  }

  private def updateLeadersList() = {
    zookeeperService.getChildrenWithData(config.leadersPath, None).flatMap { leaderNodes =>
      val map = leaderNodes.values.map(_.decodedData).toMap
      leaders.set(map)
    }
  }

}

object CacheClient {
  def create(config: ZookeeperConfig): ZIO[Any, DomainError, CacheClient] = {
    for {
      hub        <- Hub.unbounded[WatcherEvent]()
      connection <- ZKConnection.connect(config.host, config.port)
      zookeeperService = ApacheZookeeperServiceImpl(connection, hub)
      leaderNodes <- zookeeperService.getChildrenWithData(config.leadersPath, None)
      leaders = leaderNodes.values.map(_.decodedData).toMap
      leadersRef <- Ref.make(leaders)
      backend1    <- HttpClientZioBackend().mapError(ex => HttpClientError(ex.getMessage))
      backend2    <- HttpClientZioBackend().mapError(ex => HttpClientError(ex.getMessage))
      backend3    <- HttpClientZioBackend().mapError(ex => HttpClientError(ex.getMessage))
      backend4    <- HttpClientZioBackend().mapError(ex => HttpClientError(ex.getMessage))
      httpClient1 = HttpClient(backend1)
      httpClient2 = HttpClient(backend2)
      httpClient3 = HttpClient(backend3)
      httpClient4 = HttpClient(backend4)
      pool = new HttpClientPool(List(httpClient1, httpClient2, httpClient3, httpClient4))
    } yield CacheClient(pool, zookeeperService, leadersRef, config, ConsistentHashing(leaders.keys.toList))
  }
  
  val live: ZLayer[ZookeeperConfig, DomainError, CacheClient] = ZLayer.fromZIO {
    for {
      config <- ZIO.service[ZookeeperConfig]
      client <- create(config)
    } yield client
  }
}
