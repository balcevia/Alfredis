package com.alfredis.cache.server.http.endpoint

import com.alfredis.cache.server.http.CacheRoutes.Env
import CreateEntryEndpoint.publicEndpoint
import com.alfredis.cache.server.http.TapirRoute
import com.alfredis.cache.server.http.error.{CacheServerError, ErrorHandler}
import com.alfredis.cache.server.http.service.CacheService
import sttp.model.StatusCode
import sttp.tapir.Endpoint
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir.{ZServerEndpoint, *}
import zio.ZIO

object GetEntryEndpoint extends TapirRoute with ErrorHandler {

  val endpointDefinition: Endpoint[Unit, String, CacheServerError, Option[Array[Byte]], Any] = publicEndpoint.get
    .in("cache" / path[String]("key"))
    .out(
      jsonBody[Option[Array[Byte]]].and(statusCode(StatusCode.Ok)),
    )

  val serverEndpoint: ZServerEndpoint[Env, Any] = endpointDefinition.zServerLogic(request => handler(request))

  private def handler(key: String): ZIO[Env, CacheServerError, Option[Array[Byte]]] =
    ZIO.logInfo(s"Retrieving data from cache by key: $key") *> ZIO.serviceWithZIO[CacheService](_.get(key)).tap(data => ZIO.logInfo(s"FoundData: $data"))
}
