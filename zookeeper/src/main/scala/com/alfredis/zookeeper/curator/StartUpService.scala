package com.alfredis.zookeeper.curator

import zio.ZIO

trait StartUpService {
  def startup(): ZIO[Any, Nothing, Unit]
}
