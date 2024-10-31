package com.alfredis.api.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema

case class CreateEntryRequest(from: Option[String], entries: Seq[CacheEntryRequest])

object CreateEntryRequest:
  given Codec[CreateEntryRequest]  = deriveCodec
  given Schema[CreateEntryRequest] = Schema.derived
