package com.alfredis.cache.server.http.endpoint

import com.alfredis.error.UnauthorizedCacheCreateEntryRequest
import com.alfredis.cache.server.http.CacheRoutes.Env
import GetEntryEndpoint.publicEndpoint
import com.alfredis.api.model.CacheEntryResponse
import com.alfredis.cache.server.http.TapirRoute
import com.alfredis.cache.server.http.error.{CacheServerError, ErrorHandler}
import com.alfredis.cache.server.http.service.CacheService
import com.alfredis.zookeepercore.config.ZookeeperClusterState
import sttp.model.StatusCode
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir.*
import zio.{Ref, ZIO}

object GetLeadersStateEndpoint extends TapirRoute with ErrorHandler {

  val endpointDefinition = publicEndpoint.get
    .in("cache" / "leader" / "state")
    .in(header[Option[String]]("From"))
    .out(
      jsonBody[List[CacheEntryResponse]].and(statusCode(StatusCode.Ok)),
    )

  val serverEndpoint: ZServerEndpoint[Env, Any] = endpointDefinition.zServerLogic(request => handler(request))

  private def handler(from: Option[String]): ZIO[Env, CacheServerError, List[CacheEntryResponse]] = {
    val result = for {
      clusterState <- ZIO.service[Ref[ZookeeperClusterState]]
      isAuthorized <- clusterState.get.map(s => s.isLeader && s.workers.exists(w => from.contains(w.path)))
      entries <-
        if (isAuthorized) ZIO.serviceWithZIO[CacheService](_.getAll).map(_.map(r => CacheEntryResponse(r.key, r.value)))
        else ZIO.fail(UnauthorizedCacheCreateEntryRequest)
    } yield entries

    result.mapError(handleError)
  }

}
