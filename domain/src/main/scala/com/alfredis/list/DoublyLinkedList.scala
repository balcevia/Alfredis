package com.alfredis.list

case class DoublyLinkedList[T]() {
  private var _head: Node[T] = null
  private var _last: Node[T] = null

  def head: Node[T] = _head
  def last: Node[T] = _last

  def setHead(node: Node[T]): Node[T] = {
    if (_head == null) {
      _head = node
      _last = node
    } else {
      val prev = _head
      _head = node
      prev.prev = _head
      _head.next = prev
    }
    _head
  }

  def delete(node: Node[T]): Unit = {
    val prev = node.prev
    val next = node.next

    if (prev != null) {
      prev.next = next
    } else {
      _head = next
    }

    if (next != null) {
      next.prev = prev
    } else {
      _last = prev
    }
  }

  def collectAllElements(): List[Node[T]] = {
    def innerCollect(node: Node[T]): List[Node[T]] = {
      if (node == null) List.empty
      else {
        node :: innerCollect(node.next)
      }
    }
    innerCollect(_head)
  }

}
