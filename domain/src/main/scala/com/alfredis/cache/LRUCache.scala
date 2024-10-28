package com.alfredis.cache

import com.alfredis.list.{DoublyLinkedList, Node}

import java.util
import scala.jdk.CollectionConverters.*

case class LRUCache[K, V](private val capacity: Int) extends Cache[K, V] {

  private val cacheMap: util.Map[K, Node[CacheRecord[K, V]]] = new util.HashMap[K, Node[CacheRecord[K, V]]]()
  private val queue: DoublyLinkedList[CacheRecord[K, V]]     = DoublyLinkedList()

  override def put(key: K, value: V): V = {
    if (cacheMap.containsKey(key)) {
      val node = cacheMap.get(key)
      node.record.value = value

      queue.delete(node)
      queue.setHead(node)
    } else {
      if (cacheMap.size() >= capacity) {
        cacheMap.remove(queue.last.record.key)
        queue.delete(queue.last)
      }

      val node = Node(CacheRecord(key, value))
      cacheMap.put(key, node)
      queue.setHead(node)
    }

    value
  }

  override def get(key: K): Option[V] = {
    if (!cacheMap.containsKey(key)) {
      None
    } else {
      val node = cacheMap.get(key)

      queue.delete(node)
      queue.setHead(node)

      Some(node.record.value)
    }
  }

  override def getAll: List[CacheRecord[K, V]] = cacheMap.values().asScala.map(_.record).toList
}

object LRUCache {
  def apply[K, V](capacity: Int): LRUCache[K, V] = new LRUCache[K, V](capacity)
}
