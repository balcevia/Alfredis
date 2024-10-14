package com.alfredis.cache

trait Cache[K, V] {
  def put(key: K, value: V): V
  def get(key: K): Option[V]
}
