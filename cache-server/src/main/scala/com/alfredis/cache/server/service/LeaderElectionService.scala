package com.alfredis.cache.server.service

import com.alfredis.error.DomainError
import zio.IO

trait LeaderElectionService {
  def startElection(): IO[DomainError, Unit]
  def electNewLeader(): IO[DomainError, Unit]
}
