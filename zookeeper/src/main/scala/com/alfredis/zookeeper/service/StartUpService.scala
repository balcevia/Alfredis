package com.alfredis.zookeeper.service

import com.alfredis.error.DomainError
import zio.ZIO

trait StartUpService {
  def startup(): ZIO[Any, DomainError, Unit]
}
