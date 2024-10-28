package com.alfredis.cache.server

import com.alfredis.cache.server.http.service.CacheService
import com.alfredis.cache.server.service.{LeaderElectionService, WorkerRegistrationService}
import com.alfredis.error.DomainError
import zio.ZIO

object ZookeeperServer {
  def run(): ZIO[CacheService & WorkerRegistrationService & LeaderElectionService, DomainError, Unit] = {
    for {
      leaderElectionService     <- ZIO.service[LeaderElectionService]
      workerRegistrationService <- ZIO.service[WorkerRegistrationService]
      cacheService              <- ZIO.service[CacheService]
      _                         <- leaderElectionService.startElection()
      _                         <- workerRegistrationService.registerWorkerNode()
      _                         <- cacheService.retrieveStateFromLeaderIfNeeded()
      _                         <- ZIO.never
    } yield ()
  }
}
