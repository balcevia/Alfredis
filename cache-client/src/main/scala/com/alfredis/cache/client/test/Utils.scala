package com.alfredis.cache.client.test

import java.util.UUID

object Utils {
  private def randomString(length: Int) = {
    val r  = new scala.util.Random
    val sb = new StringBuilder
    for (i <- 1 to length)
      sb.append(r.nextPrintableChar)
    sb.toString
  }

  def createTestData(numberOfRecords: Int, stringLength: Int): Map[String, Array[Byte]] = {
    (0 until numberOfRecords).map { _ =>
      val key   = UUID.randomUUID().toString
      val value = randomString(stringLength).getBytes
      key -> value
    }.toMap
  }

  def createTestDataWithByteKey(numberOfRecords: Int, stringLength: Int): Map[Array[Byte], Array[Byte]] = {
    createTestData(numberOfRecords, stringLength).map { case (key, value) => key.getBytes -> value }
  }
}
