package com.alfredis.zookeeper.service

import com.alfredis.error.DomainError
import zio.ZIO

trait ZKZioService {
  def getChildren(path: String, watcher: Boolean): ZIO[Any, DomainError, List[String]]
  def get(path: String): ZIO[Any, DomainError, Option[Array[Byte]]]
}
