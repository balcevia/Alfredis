package com.alfredis.console.client

import scala.util.matching.Regex

sealed trait Command

case class PutCommand(key: String, value: String) extends Command
case class GetCommand(key: String) extends Command
case object ExitCommand extends Command

object Command {
  val EXIT_COMMAND_REGEX: Regex = "exit".r
  val PUT_COMMAND_REGEX: Regex = """put\s+(\w+)->(.+)""".r
  val GET_COMMAND_REGEX: Regex = """get\s+(\w+)""".r
}