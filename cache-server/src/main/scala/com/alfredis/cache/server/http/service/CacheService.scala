package com.alfredis.cache.server.http.service

import com.alfredis.cache.CacheRecord
import com.alfredis.cache.server.http.model.CacheEntryRequest
import zio.UIO

trait CacheService {
  def put(entries: Seq[CacheEntryRequest], isLeader: Boolean): UIO[Unit]
  def get(key: String): UIO[Option[Array[Byte]]]
  def getAll: UIO[List[CacheRecord[String, Array[Byte]]]]
  def retrieveStateFromLeaderIfNeeded(): UIO[Unit]
}