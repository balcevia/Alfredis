package com.alfredis.tcp

import com.alfredis.api.model.CacheEntryRequest

case class TCPRequest(
    requestType: TCPRequestType,
    key: Option[String] = None,
    entity: List[CacheEntryRequest] = Nil,
    resource: Option[String] = None,
)

enum TCPRequestType:
  case GET, PUT, GET_ALL
