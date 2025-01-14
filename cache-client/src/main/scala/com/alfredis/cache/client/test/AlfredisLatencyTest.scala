package com.alfredis.cache.client.test

import com.alfredis.cache.client.CacheClient
import com.alfredis.zookeepercore.config.ZookeeperConfig
import zio.{Clock, ZIO, ZIOAppDefault}

import java.util.concurrent.TimeUnit

object AlfredisLatencyTest extends ZIOAppDefault {
  /*
   * We need to divide tests into groups depending on configuration:
   * initial assumption is to make tests depending on number of replicas and number of lieder nodes
   * For latency measurements we need a chart of response time vs number of requests, we measure for example 50 requests response time, it's a first point, then 100 req, 150 and so on.

   * Make two separate charts for put and get

   * Think about test depending on data volume
   */

  override def run = runTests(1000, 100, 33).provide(CacheClient.live, ZookeeperConfig.live)

  private def runTests(numberOfRequests: Int, step: Int, stringLength: Int) = for {
    results <- latencyTest(numberOfRequests, step, stringLength)
  } yield {
    println("Test finished, results:")
    results.foreach(println)
  }

  def measurePut(data: Map[String, Array[Byte]]): ZIO[CacheClient, Nothing, Long] = {
    for {
      result         <- sendPutRequests(data)
    } yield result
  }

  private def sendPutRequests(data: Map[String, Array[Byte]]): ZIO[CacheClient, Nothing, Long] = for {
    client <- ZIO.service[CacheClient]
    measures      <- ZIO.collectAllPar(data.map {
      case (key, value) =>
        for {
          startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
          _ <- client.put(key, value)
          endTime   <- Clock.currentTime(TimeUnit.MILLISECONDS)
        } yield endTime - startTime
    })
  } yield {
    val length = measures.size

    measures.sum / length
  }

  def latencyTest(numberOfRequests: Int, step: Int, stringLength: Int): ZIO[CacheClient, Nothing, List[LatencyTestResult]] = {
    ZIO.collectAll(
      (0 to numberOfRequests by step).tail.toList.map { num =>
        val testData = Utils.createTestData(num, stringLength)

        measurePut(testData).map(millis => LatencyTestResult(num, millis))
      },
    )
  }
}
