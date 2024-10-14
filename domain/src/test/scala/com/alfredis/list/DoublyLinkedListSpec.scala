package com.alfredis.list

import com.alfredis.cache.CacheRecord
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DoublyLinkedListSpec extends AnyFlatSpec with Matchers {
  behavior of "DoublyLinkedList"

  it should "add new elements" in {
    val list = DoublyLinkedList[CacheRecord[String, Int]]()
    for (i <- 5 to 1 by -1)
      list.setHead(Node(CacheRecord(i.toString, i)))

    list.collectAllElements().map(_.record.value) shouldEqual List(1, 2, 3, 4, 5)
  }

  it should "add new elements and remove nodes correctly" in {
    val list = DoublyLinkedList[CacheRecord[String, Int]]()

    val node1 = list.setHead(Node(CacheRecord("1", 1)))
    val node2 = list.setHead(Node(CacheRecord("2", 2)))
    val node3 = list.setHead(Node(CacheRecord("3", 3)))
    val node4 = list.setHead(Node(CacheRecord("4", 4)))

    // list looks like List(4, 3, 2, 1)

    list.last shouldEqual node1

    list.delete(node2)

    list.collectAllElements().map(_.record.value) shouldEqual List(4, 3, 1)

    list.delete(node4)

    list.collectAllElements().map(_.record.value) shouldEqual List(3, 1)

    list.delete(node1)

    list.last shouldEqual node3

    list.collectAllElements().length shouldEqual 1

    list.collectAllElements().head shouldEqual node3

    list.setHead(Node(CacheRecord("5", 5)))

    list.setHead(Node(CacheRecord("6", 6)))

    list.last.record.value shouldEqual 3

    list.delete(node3)

    list.last.record.value shouldEqual 5

    list.collectAllElements().map(_.record.value) shouldEqual List(6, 5)
  }
}
