package com.alfredis.cache.server.http

object URLUtils {
  def createEntryUrl(host: String): String = s"http://$host/cache/create"
  def retrieveLeadersStateUrl(host: String): String = s"http://$host/cache/leader/state"
}
