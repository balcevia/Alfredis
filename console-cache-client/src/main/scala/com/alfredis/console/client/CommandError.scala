package com.alfredis.console.client

sealed trait CommandError {
  val message: String
}

case class InvalidCommand(cmd: String) extends CommandError {
  override val message: String = s"Received invalid command: $cmd"
}
