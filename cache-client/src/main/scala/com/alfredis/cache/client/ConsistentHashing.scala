package com.alfredis.cache.client

import java.security.MessageDigest
import scala.collection.immutable.TreeMap

case class ConsistentHashing(nodes: List[String], replicas: Int = 3, hashingAlgorithm: String = "MD5") {
  private val messageDigest: MessageDigest = MessageDigest.getInstance(hashingAlgorithm)

  private var hashRing: TreeMap[Long, String] = {
    val keyValues = nodes.flatMap(node => (1 to replicas).map(createRingEntry(node, _)).toList)

    val result = TreeMap.from(keyValues)

    result.toList.sortBy(_._1).foreach(println) // todo remove

    result
  }

  private def createRingEntry(node: String, index: Int): (Long, String) = generateHash(s"$node-$index") -> node

  private def generateHash(key: String): Long = {
    messageDigest.reset()
    messageDigest.update(key.getBytes())
    val digest = messageDigest.digest()

    (digest(3) & 0xff) << 24 | ((digest(2) & 0xff) << 16) | ((digest(1) & 0xff) << 8) | (digest(0) & 0xff)
  }

  def addNode(node: String): Unit = {
    hashRing = hashRing ++ (1 to replicas).map(createRingEntry(node, _))
  }

  def removeNode(node: String): Unit = {
    hashRing = hashRing.removedAll((1 to replicas).map(createRingEntry(node, _)).map(_._1))
  }

  def getNode(key: String): String = {
    val hash = generateHash(key)
    val min  = hashRing.minAfter(hash).map(_._2)
    min.getOrElse(hashRing.head._2)
  }
}
