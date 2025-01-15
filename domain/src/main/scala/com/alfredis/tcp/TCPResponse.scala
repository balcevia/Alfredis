package com.alfredis.tcp

import com.alfredis.api.model.CacheEntryResponse

case class TCPResponse(status: TCPResponseStatus, data: List[CacheEntryResponse] = Nil)

enum TCPResponseStatus:
  case OK, BadRequest, Unauthorized
