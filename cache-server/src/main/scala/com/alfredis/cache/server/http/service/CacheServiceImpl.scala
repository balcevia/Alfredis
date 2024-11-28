package com.alfredis.cache.server.http.service

import com.alfredis.api.model.{CacheEntryRequest, CacheEntryResponse, CreateEntryRequest}
import com.alfredis.cache.server.http.URLUtils
import com.alfredis.cache.{Cache, CacheRecord}
import com.alfredis.httpclient.HttpClient
import com.alfredis.zookeepercore.config.ZookeeperClusterState
import io.circe.Printer
import io.circe.syntax.*
import sttp.model.Header
import zio.{Ref, UIO, ZIO, ZLayer}

case class CacheServiceImpl(cache: Ref[Cache[String, Array[Byte]]], clusterState: Ref[ZookeeperClusterState], httpClient: HttpClient)
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
      requestBody    = CreateEntryRequest(state.currentLeader.map(_.path), entries)
      serialisedBody = requestBody.asJson.printWith(Printer.noSpaces)
      requests       = state.workers.map(w => URLUtils.createEntryUrl(w.decodedData._2)).map(uri => httpClient.postRequest(uri, serialisedBody))
      _ <- ZIO.collectAllPar(requests.map(httpClient.callApiUnit))
    } yield ()

    result.foldZIO(
      error => ZIO.logError(s"Replication failed: ${error.message}") *> ZIO.unit,
      _ => ZIO.logInfo(s"Replication succeeded") *> ZIO.unit,
    )
  }

  override def get(key: String): UIO[Option[Array[Byte]]] = cache.get.map(_.get(key))

  override def getAll: UIO[List[CacheRecord[String, Array[Byte]]]] = cache.get.map(_.getAll)

  override def retrieveStateFromLeaderIfNeeded(): UIO[Unit] = {
    val result = clusterState.get.flatMap { state =>
      if (!state.isLeader && state.currentLeader.isDefined) {
        val request = httpClient
          .getRequest(URLUtils.retrieveLeadersStateUrl(state.currentLeader.get.decodedData._2))
          .header(Header("From", state.workerNode.get.path))

        for {
          response <- httpClient.callApi[List[CacheEntryResponse]](request)
          _        <- ZIO.logInfo(s"Received data from the leader: $response")
          _ <- cache.update { c =>
            response.foreach(e => c.put(e.key, e.value)); c
          }
        } yield ()
      } else ZIO.logInfo("Current node is a leader, data sync is not needed") *> ZIO.unit
    }

    result.foldZIO(
      error => ZIO.logError(error.message),
      v => ZIO.succeed(v),
    )
  }
}

object CacheServiceImpl {
  val live: ZLayer[HttpClient & Ref[ZookeeperClusterState] & Ref[Cache[String, Array[Byte]]], Nothing, CacheServiceImpl] =
    ZLayer.fromZIO {
      for {
        cache        <- ZIO.service[Ref[Cache[String, Array[Byte]]]]
        clusterState <- ZIO.service[Ref[ZookeeperClusterState]]
        httpClient   <- ZIO.service[HttpClient]
      } yield CacheServiceImpl(cache, clusterState, httpClient)
    }
}
