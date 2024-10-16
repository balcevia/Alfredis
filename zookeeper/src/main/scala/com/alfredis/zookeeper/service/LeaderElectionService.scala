package com.alfredis.zookeeper.service

import com.alfredis.error.DomainError
import zio.IO

trait LeaderElectionService {
  def startElection(): IO[DomainError, Unit]
  def electNewLeader(): IO[DomainError, Unit]
}
