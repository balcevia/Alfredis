package com.alfredis.zookeeper.model

case class WatcherEvent(eventType: WatcherEventType, path: String)

enum WatcherEventType:
  case ChildrenChange
