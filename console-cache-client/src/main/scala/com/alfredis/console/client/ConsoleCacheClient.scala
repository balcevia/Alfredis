package com.alfredis.console.client

import cats.syntax.either.*
import zio.*
import zio.Console.*
import zio.stream.ZStream

import java.io.IOException

case class ConsoleCacheClient(processor: CommandProcessor) {
  private val welcomeMessageAndInstruction: String =
    """
      |Welcome to Console Cache Client.
      |In order to save data into the cache use put command, example: "put key->value"
      |In order to get data from cache use get command, example: "get key"
      |Use exit command to exit Console Cache Client.
      |""".stripMargin

  def run: ZIO[Any, IOException, Unit] = for {
    _ <- Console.printLine(welcomeMessageAndInstruction)
    _ <- commandStream(processor.process).runDrain
  } yield ()

  private def mapCommand(cmd: String): Either[CommandError, Command] = {
    cmd match {
      case Command.PUT_COMMAND_REGEX(key, value) => PutCommand(key, value).asRight
      case Command.GET_COMMAND_REGEX(key)        => GetCommand(key).asRight
      case Command.EXIT_COMMAND_REGEX()          => ExitCommand.asRight
      case _                                     => InvalidCommand(cmd).asLeft
    }
  }

  private def commandStream(process: Command => URIO[Any, Either[Throwable, Unit]]): ZStream[Any, IOException, Unit] =
    ZStream
      .repeatZIO(Console.readLine)
      .map(mapCommand)
      .takeWhile {
        case Right(ExitCommand) => false
        case _                  => true
      }
      .mapZIO {
        case Right(cmd) =>
          process(cmd).flatMap {
            case Right(())   => Console.printLine(s"Successfully processed: $cmd")
            case Left(error) => Console.printLine(s"Failed to process command: $cmd, reason: ${error.getMessage}")
          }
        case Left(error) => Console.printLine(error.message)
      }
}

object ConsoleCacheClient {
  val live: ZLayer[CommandProcessor, Nothing, ConsoleCacheClient] = ZLayer.fromFunction(ConsoleCacheClient(_))
}