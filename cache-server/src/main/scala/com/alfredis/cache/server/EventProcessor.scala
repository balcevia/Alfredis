package com.alfredis.cache.server

import com.alfredis.cache.server.service.WatcherEventProcessor
import com.alfredis.error.DomainError
import zio.ZIO

object EventProcessor {
  def run(): ZIO[WatcherEventProcessor, DomainError, Unit] = {
    for {
      eventProcessor <- ZIO.service[WatcherEventProcessor]
      _              <- eventProcessor.subscribe()
    } yield ()
  }
}
