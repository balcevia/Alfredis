package com.alfredis.cache.client.rediss

import redis.clients.jedis.JedisPool

object RedisTest extends App {
  val pool = new JedisPool("localhost", 6379)
  val jedis = pool.getResource

  jedis.set("test", "First test")

  println(jedis.get("test"))
}
