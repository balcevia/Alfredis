package com.alfredis.cache.server.http

import com.alfredis.cache.server.http.error.{CacheServerError, CacheServerErrorMapping, Unknown}
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir.*
import sttp.tapir.{Endpoint, EndpointOutput}

trait TapirRoute {

  private val errorMappings: EndpointOutput.OneOf[CacheServerError, CacheServerError] = oneOf[CacheServerError](
    CacheServerErrorMapping.NotFound,
    CacheServerErrorMapping.ServerError,
    CacheServerErrorMapping.BadRequest,
    CacheServerErrorMapping.Forbidden,
    CacheServerErrorMapping.UnprocessableEntity,
    oneOfDefaultVariant(jsonBody[Unknown].description("Unmapped error response")),
  )

  val publicEndpoint: Endpoint[Unit, Unit, CacheServerError, Unit, Any] = endpoint
    .errorOut(errorMappings)

}
