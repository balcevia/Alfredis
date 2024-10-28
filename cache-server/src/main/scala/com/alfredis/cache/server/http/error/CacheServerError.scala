package com.alfredis.cache.server.http.error

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema

sealed trait CacheServerError {
  def code: String
  def message: String
}

final case class AuthorizationError private (code: String, message: String) extends CacheServerError

object AuthorizationError {
  private val code = "unauthorized"

  def apply(message: String): AuthorizationError = AuthorizationError(code, message)

  given Codec[AuthorizationError] = deriveCodec
  given Schema[AuthorizationError] = Schema.derived
}

final case class UnprocessableEntity private (code: String, message: String) extends CacheServerError

object UnprocessableEntity {
  private val code = "non-processable"

  def apply(message: String): UnprocessableEntity = UnprocessableEntity(code, message)

  given Codec[UnprocessableEntity] = deriveCodec
  given Schema[UnprocessableEntity] = Schema.derived
}

final case class NotFound private (code: String, message: String) extends CacheServerError

object NotFound {
  private val code = "not-found"

  def apply(message: String): NotFound = NotFound(code, message)

  given Codec[NotFound] = deriveCodec
  given Schema[NotFound] = Schema.derived
}

final case class BadRequest private (code: String, message: String, problems: Option[List[String]]) extends CacheServerError

object BadRequest {
  private val code = "bad-request"

  def apply(message: String): BadRequest = BadRequest(code, message, None)
  def apply(message: String, problems: Option[List[String]]): BadRequest = BadRequest(code, message, problems)

  given Codec[BadRequest] = deriveCodec
  given Schema[BadRequest] = Schema.derived
}

final case class Unknown(code: String, message: String) extends CacheServerError

object Unknown {
  given Codec[Unknown] = deriveCodec
  given Schema[Unknown] = Schema.derived
}

final case class ServerError private (code: String, message: String) extends CacheServerError

object ServerError {
  private val code = "server-error"

  def apply(message: String): ServerError = ServerError(code, message)

  given Codec[ServerError] = deriveCodec
  given Schema[ServerError] = Schema.derived
}