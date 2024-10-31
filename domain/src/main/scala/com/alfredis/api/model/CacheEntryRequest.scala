package com.alfredis.api.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema

case class CacheEntryRequest(key: String, value: Array[Byte])

object CacheEntryRequest:
  given Codec[CacheEntryRequest]  = deriveCodec
  given Schema[CacheEntryRequest] = Schema.derived
