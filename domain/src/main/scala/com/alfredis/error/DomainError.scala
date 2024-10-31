package com.alfredis.error

sealed trait DomainError {
  val message: String
}

case class ZookeeperError(override val message: String) extends DomainError

case class ZookeeperDataDeserializationError(override val message: String) extends DomainError

object ZookeeperDataDeserializationError {
  def apply(throwable: Throwable): ZookeeperDataDeserializationError = ZookeeperDataDeserializationError(throwable.getMessage)
}

object ZookeeperError {
  def apply(throwable: Throwable): ZookeeperError = ZookeeperError(throwable.getMessage)
}

case class ZookeeperConnectionError(override val message: String)   extends DomainError
case class ZookeeperNodeCreationError(override val message: String) extends DomainError
case class ZNodeNotFound(path: String) extends DomainError {
  override val message: String = s"ZNode with path $path doesn't exist"
}

case object UnauthorizedCacheCreateEntryRequest extends DomainError {
  override val message: String = "Unauthorized cache create entry request"
}

case class HttpClientError(override val message: String) extends DomainError

case class HttpClientSendingRequestError(override val message: String) extends DomainError

case class HttpClientDecodingError(override val message: String) extends DomainError
