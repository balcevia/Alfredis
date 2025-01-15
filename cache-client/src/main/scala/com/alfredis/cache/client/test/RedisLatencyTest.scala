package com.alfredis.cache.client.test

import redis.clients.jedis.JedisPool
import zio.{Clock, ZIO, ZIOAppDefault}

import java.util.concurrent.TimeUnit
import scala.collection.immutable

object RedisLatencyTest extends ZIOAppDefault {
  private val pool  = new JedisPool("localhost", 6379)
  private val jedis = pool.getResource

  override def run: ZIO[Any, Nothing, Unit] = runTests(1000, 100, 33).fold(error => println(error), _ => ())

  private def runTests(numberOfRequests: Int, step: Int, stringLength: Int) = for {
    results <- latencyTest(numberOfRequests, step, stringLength)
  } yield {
    println("Test finished, results:")
    results.foreach(println)
  }

  def measurePut(data: Map[Array[Byte], Array[Byte]]): ZIO[Any, Throwable, Long] = {
    for {
      startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      _ <- sendPutRequests(data)
      endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
    } yield endTime - startTime
  }

  private def sendPutRequests(data: Map[Array[Byte], Array[Byte]]): ZIO[Any, Throwable, immutable.Iterable[String]] = {
    ZIO.collectAll(data.map { case (key, value) => ZIO.blocking(ZIO.attempt(jedis.set(key, value))) })
  }

  def latencyTest(numberOfRequests: Int, step: Int, stringLength: Int): ZIO[Any, Throwable, List[LatencyTestResult]] = {
    ZIO.collectAll(
      (0 to numberOfRequests by step).tail.toList.map { num =>
        val testData = Utils.createTestDataWithByteKey(num, stringLength)

        measurePut(testData).map(millis => LatencyTestResult(num, millis))
      },
    )
  }
}
