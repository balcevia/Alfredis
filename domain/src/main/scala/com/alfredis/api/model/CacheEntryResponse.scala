package com.alfredis.api.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema

case class CacheEntryResponse(key: String, value: Array[Byte])

object CacheEntryResponse:
  given Codec[CacheEntryResponse]  = deriveCodec
  given Schema[CacheEntryResponse] = Schema.derived
