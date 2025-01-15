package com.alfredis.cache.server.http

import com.alfredis.cache.server.config.AppConfig
import CacheRoutes.Env
import org.http4s.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import zio.*
import zio.interop.catz.*

object CacheHttpServer {
  val serve: ZIO[Env & AppConfig, Throwable, Unit] =
    ZIO.service[AppConfig].flatMap { appConfig =>
      ZIO.executor.flatMap(executor =>
        BlazeServerBuilder[RIO[Env, *]]
          .withExecutionContext(executor.asExecutionContext)
          .bindHttp(appConfig.server.httpPort, appConfig.server.host)
          .withHttpApp(Router("/" -> CacheRoutes.routes).orNotFound)
          .serve
          .compile
          .drain,
      )
    }
}
