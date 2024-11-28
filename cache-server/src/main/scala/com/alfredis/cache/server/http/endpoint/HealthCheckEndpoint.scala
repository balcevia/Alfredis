package com.alfredis.cache.server.http.endpoint

import com.alfredis.cache.server.http.CacheRoutes.Env
import com.alfredis.cache.server.http.TapirRoute
import com.alfredis.cache.server.http.error.{CacheServerError, ErrorHandler}
import sttp.model.StatusCode
import sttp.tapir.Endpoint
import sttp.tapir.ztapir.*
import zio.ZIO

object HealthCheckEndpoint extends TapirRoute with ErrorHandler {

  val endpointDefinition: Endpoint[Unit, Unit, CacheServerError, Unit, Any] = publicEndpoint.get
    .in("health")
    .out(
      statusCode(StatusCode.Ok),
    )

  val serverEndpoint: ZServerEndpoint[Env, Any] = endpointDefinition.zServerLogic(_ => ZIO.unit)
}
