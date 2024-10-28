package com.alfredis.zookeepercore.model

case class WatcherEvent(eventType: WatcherEventType, path: String)

enum WatcherEventType:
  case ElectionStateChange, WorkersChange
