package com.alfredis.cache.server.http

import com.alfredis.cache.server.http.endpoint.{CreateEntryEndpoint, GetEntryEndpoint, GetLeadersStateEndpoint}
import com.alfredis.cache.server.http.service.CacheService
import com.alfredis.zookeepercore.config.ZookeeperClusterState
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.ztapir.*
import zio.{RIO, Ref}

object CacheRoutes {
  type Env = CacheService & Ref[ZookeeperClusterState]

  val routes: HttpRoutes[RIO[Env, *]] =
    ZHttp4sServerInterpreter()
      .from(
        List(
          CreateEntryEndpoint.serverEndpoint,
          GetEntryEndpoint.serverEndpoint,
          GetLeadersStateEndpoint.serverEndpoint,
        ).map(_.widen[Env]),
      )
      .toRoutes
}
