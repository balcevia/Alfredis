package com.alfredis.cache.client

import com.alfredis.httpclient.HttpClient

import java.util.concurrent.atomic.AtomicInteger

class HttpClientPool(clients: List[HttpClient]) {
  private val counter: AtomicInteger = new AtomicInteger(0)

  def next: HttpClient = {
    val index = counter.get() % clients.length
    counter.incrementAndGet()
    clients(index)
  }
}
