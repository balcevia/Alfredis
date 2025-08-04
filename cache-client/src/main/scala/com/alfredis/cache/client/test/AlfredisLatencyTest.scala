package com.alfredis.cache.client.test

import com.alfredis.cache.client.AlfredisCacheClientImpl
import com.alfredis.tcp.ZIOTCPClient
import com.alfredis.zookeepercore.config.ZookeeperConfig
import zio.{Clock, ZIO, ZIOAppDefault}

import java.util.concurrent.TimeUnit

object AlfredisLatencyTest extends ZIOAppDefault {
  /*
   * I need to divide tests into groups depending on configuration:
   * initial assumption is to make tests depending on number of replicas and number of lieder nodes
   * For latency measurements I need a chart of response time vs number of requests, I measure for example 50 requests response time, it's a first point, then 100 req, 150 and so on.

   * Make two separate charts for put and get

   * Think about test depending on data volume
   */

  override def run = runTests(1000, 100, 33).provide(AlfredisCacheClientImpl.live, ZookeeperConfig.live, ZIOTCPClient.live)

  private def runTests(numberOfRequests: Int, step: Int, stringLength: Int) = for {
    results <- latencyTest(numberOfRequests, step, stringLength)
  } yield {
    println("Test finished, results:")
    results.foreach(println)
  }

  def measurePut(data: Map[String, Array[Byte]]): ZIO[AlfredisCacheClientImpl, Throwable, Long] = {
    for {
      client <- ZIO.service[AlfredisCacheClientImpl]
      startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      _         <- sendPutRequests(client, data)
      endTime   <- Clock.currentTime(TimeUnit.MILLISECONDS)
    } yield endTime - startTime
  }

  private def sendPutRequests(client: AlfredisCacheClientImpl, data: Map[String, Array[Byte]]): ZIO[AlfredisCacheClientImpl, Throwable, Unit] = for {
    _ <- ZIO.collectAllPar(data.map { case (key, value) => client.put(key, value) })
  } yield ()

  def latencyTest(numberOfRequests: Int, step: Int, stringLength: Int): ZIO[AlfredisCacheClientImpl, Throwable, List[LatencyTestResult]] = {
    ZIO.collectAll(
      (0 to numberOfRequests by step).tail.toList.map { num =>
        val testData = Utils.createTestData(num, stringLength)

        measurePut(testData).map(millis => LatencyTestResult(num, millis))
      },
    )
  }
}
