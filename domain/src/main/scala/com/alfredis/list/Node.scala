package com.alfredis.list

case class Node[T](record: T) {
  var prev: Node[T] = null
  var next: Node[T] = null
}
