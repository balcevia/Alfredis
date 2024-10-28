package com.alfredis.cache.server.http.error

import sttp.model.StatusCode
import sttp.tapir.EndpointOutput
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir.oneOfVariant

object CacheServerErrorMapping {
  val BadRequest: EndpointOutput.OneOfVariant[BadRequest] = oneOfVariant(
    StatusCode.BadRequest,
    jsonBody[BadRequest].description("'Bad Request' api error is returned when request can not be processed properly"),
  )

  val Forbidden: EndpointOutput.OneOfVariant[AuthorizationError] = oneOfVariant(
    StatusCode.Forbidden,
    jsonBody[AuthorizationError].description(
      "'Forbidden' api error is returned when user is unauthorized or doesn't have enough permissions",
    ),
  )

  val NotFound: EndpointOutput.OneOfVariant[NotFound] = oneOfVariant(
    StatusCode.NotFound,
    jsonBody[NotFound].description("'NotFound' api error is returned when requested resource is not found"),
  )

  val UnprocessableEntity: EndpointOutput.OneOfVariant[UnprocessableEntity] = oneOfVariant(
    StatusCode.UnprocessableEntity,
    jsonBody[UnprocessableEntity]
      .description(
        "'UnprocessableEntity' api error is returned when request body is correct, but server was not able to process the instructions",
      ),
  )

  val ServerError: EndpointOutput.OneOfVariant[ServerError] = oneOfVariant(
    StatusCode.InternalServerError,
    jsonBody[ServerError].description("Internal server error"),
  )
}
