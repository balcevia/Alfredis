package com.alfredis.console.client

import com.alfredis.cache.client.CacheClient
import zio.{Console, URIO, ZIO, ZLayer}

trait CommandProcessor {
  def process(cmd: Command): URIO[Any, Either[Throwable, Unit]]
}

case class CommandProcessorImpl(client: CacheClient) extends CommandProcessor {
  override def process(cmd: Command): URIO[Any, Either[Throwable, Unit]] = cmd match {
    case PutCommand(key, value) => client.put(key, value.getBytes).either
    case GetCommand(key) =>
      client
        .get(key)
        .flatMap {
          case Some(bytes) =>
            val str = new String(bytes)
            Console.printLine(s"Cache returned value: $str, for key: $key")
          case None => Console.printLine(s"Entry with key: $key, doesn't exist in cache")
        }
        .either
    case _ => ZIO.succeed(Right(()))
  }
}

object CommandProcessor {
  val live: ZLayer[CacheClient, Nothing, CommandProcessor] = ZLayer.fromFunction(CommandProcessorImpl(_))
}
