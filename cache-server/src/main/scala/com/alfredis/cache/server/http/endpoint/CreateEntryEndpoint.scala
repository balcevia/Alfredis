package com.alfredis.cache.server.http.endpoint

import com.alfredis.api.model.CreateEntryRequest
import com.alfredis.cache.server.http.TapirRoute
import com.alfredis.cache.server.http.error.{CacheServerError, ErrorHandler}
import com.alfredis.cache.server.http.service.CacheService
import com.alfredis.error.UnauthorizedCacheCreateEntryRequest
import com.alfredis.cache.server.http.CacheRoutes.Env
import com.alfredis.zookeepercore.config.ZookeeperClusterState
import sttp.model.StatusCode
import sttp.tapir.Endpoint
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir.*
import zio.{Ref, ZIO}

object CreateEntryEndpoint extends TapirRoute with ErrorHandler {

  val endpointDefinition: Endpoint[Unit, CreateEntryRequest, CacheServerError, Unit, Any] = publicEndpoint.post
    .in("cache" / "create")
    .in(
      jsonBody[CreateEntryRequest]
        .description("Endpoint which allows to create cache entries"),
    )
    .out(
      statusCode(StatusCode.Created)
        .description("Endpoint returns CREATED status if entries where created"),
    )

  val serverEndpoint: ZServerEndpoint[Env, Any] = endpointDefinition.zServerLogic(request => handler(request))

  private def handler(request: CreateEntryRequest): ZIO[Env, CacheServerError, Unit] = {
    val result = for {
      clusterState <- ZIO.service[Ref[ZookeeperClusterState]]
      state        <- clusterState.get
      isAuthorized = state.isLeader || request.from == state.currentLeader.map(_.path)
      _ <- ZIO.logTrace("Creating new entries...")
      _ <-
        if (isAuthorized) ZIO.serviceWithZIO[CacheService](_.put(request.entries, state.isLeader))
        else
          ZIO.logTrace("Creating new entries failed, client is not authorizes to create new entries") *> ZIO.fail(
            UnauthorizedCacheCreateEntryRequest,
          )
    } yield ()

    result.mapError(handleError)
  }

}
