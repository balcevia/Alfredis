package com.alfredis.cache

import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LRUCacheSpec extends AnyFlatSpec with Matchers with OptionValues {
  behavior of "LRUCache"

  it should "add elements to cache and return them" in {
    val cache: Cache[String, Int] = LRUCache[String, Int](5, 100)

    for (i <- 1 to 5)
      cache.put(i.toString, i)

    for (i <- 1 to 5)
      cache.get(i.toString).value shouldEqual i
  }

  it should "add elements and remove least recent used if number of records exceeds capacity" in {
    val cache: Cache[String, Int] = LRUCache[String, Int](5, 100)

    for (i <- 1 to 5)
      cache.put(i.toString, i)

    cache.get("1")
    cache.put("6", 6)

    cache.get("2") shouldEqual None

    cache.put("3", 7)
    cache.put("8", 8)

    cache.get("4") shouldEqual None
    cache.get("3").value shouldEqual 7
  }
}
