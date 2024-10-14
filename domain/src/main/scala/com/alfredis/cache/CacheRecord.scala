package com.alfredis.cache

case class CacheRecord[K, V](key: K, var value: V, metadata: Option[CacheRecordMetadata] = None)

case class CacheRecordMetadata(ttl: Option[Long])
