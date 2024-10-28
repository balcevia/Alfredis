package com.alfredis.cache.server.service

import com.alfredis.error.DomainError
import zio.IO

trait WorkerRegistrationService {
  def registerWorkerNode(): IO[DomainError, Unit]
}
