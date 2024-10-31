package com.alfredis.cache.server.http.error

import com.alfredis.error.*

trait ErrorHandler {
  def handleError(error: DomainError): CacheServerError = error match {
    case ZookeeperError(message)                     => ServerError(s"Zookeeper error occurred: $message")
    case ZookeeperDataDeserializationError(message)  => ServerError(s"Zookeeper data serialization error occurred: $message")
    case ZookeeperConnectionError(message)           => ServerError(s"Zookeeper connection error occurred: $message")
    case ZookeeperNodeCreationError(message)         => ServerError(s"Zookeeper node creation error occurred: $message")
    case ZNodeNotFound(message)                      => ServerError(message)
    case error @ UnauthorizedCacheCreateEntryRequest => AuthorizationError(error.message)
    case HttpClientError(message)                    => BadRequest(message)
    case HttpClientSendingRequestError(message)      => BadRequest(message)
    case HttpClientDecodingError(message)            => UnprocessableEntity(message)
  }
}
