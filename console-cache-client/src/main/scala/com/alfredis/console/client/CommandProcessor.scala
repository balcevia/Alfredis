package com.alfredis.console.client

import com.alfredis.cache.client.CacheClient
import zio.{Clock, Console, URIO, ZIO, ZLayer}

import java.util.concurrent.TimeUnit

trait CommandProcessor {
  def process(cmd: Command): URIO[Any, Either[Throwable, Unit]]
}

case class CommandProcessorImpl(client: CacheClient) extends CommandProcessor {
  override def process(cmd: Command): URIO[Any, Either[Throwable, Unit]] = cmd match {
    case PutCommand(key, value) =>
      measure(cmd, client.put(key, value.getBytes)).either
    case GetCommand(key) =>
      measure(cmd, client.get(key))
        .flatMap {
          case Some(bytes) =>
            val str = new String(bytes)
            Console.printLine(s"Cache returned value: $str, for key: $key")
          case None => Console.printLine(s"Entry with key: $key, doesn't exist in cache")
        }
        .either
    case _ => ZIO.succeed(Right(()))
  }
  
  private def measure[T](cmd: Command, operation: ZIO[Any, Throwable, T]): ZIO[Any, Throwable, T] = {
    for {
      startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      result <- operation
      endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      _ <- Console.printLine(s"$cmd took ${endTime - startTime} milliseconds to execute.")
    } yield result
  }
}

object CommandProcessor {
  val live: ZLayer[CacheClient, Nothing, CommandProcessor] = ZLayer.fromFunction(CommandProcessorImpl(_))
}
