package com.alfredis.cache.server.tcp

import com.alfredis.api.model.CacheEntryResponse
import com.alfredis.cache.server.config.AppConfig
import com.alfredis.cache.server.http.service.CacheService
import com.alfredis.error.UnauthorizedCacheCreateEntryRequest
import com.alfredis.tcp.TCPRequestType.{GET, GET_ALL, PUT}
import com.alfredis.tcp.TCPResponseStatus.{BadRequest, OK, Unauthorized}
import com.alfredis.tcp.{TCPRequest, TCPResponse}
import com.alfredis.zookeepercore.config.ZookeeperClusterState
import zio.{Ref, URIO, ZIO, ZLayer}

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.{ServerSocket, Socket}

case class ZIOTCPServer(appConfig: AppConfig, cacheService: CacheService, clusterState: Ref[ZookeeperClusterState]) {

  def start(): ZIO[Any, Throwable, Unit] = for {
    _            <- ZIO.logTrace(s"Starting TCP server on port ${appConfig.server.port}")
    serverSocket <- ZIO.attempt(new ServerSocket(appConfig.server.port))
    _            <- accept(serverSocket)
  } yield ()

  private def accept(serverSocket: ServerSocket) = ZIO
    .blocking(ZIO.attempt(serverSocket.accept()))
    .flatMap { socket =>
      for {
        fiber <- handle(socket).fork
        _     <- fiber.join
      } yield ()
    }
    .forever

  private def handle(socket: Socket): ZIO[Any, Throwable, Unit] = for {
    _                  <- ZIO.logTrace(s"Handling request...")
    objectOutputStream <- ZIO.attempt(new ObjectOutputStream(socket.getOutputStream))
    objectInputStream  <- ZIO.attempt(new ObjectInputStream(socket.getInputStream))
    requestEntity      <- ZIO.attempt(objectInputStream.readObject().asInstanceOf[TCPRequest])
    _                  <- process(requestEntity, objectOutputStream, objectInputStream, socket)
  } yield ()

  private def writeResponseAndHandleConnections(
      response: TCPResponse,
      objectOutputStream: ObjectOutputStream,
      objectInputStream: ObjectInputStream,
      socket: Socket,
  ): ZIO[Any, Throwable, Unit] = for {
    _ <- ZIO.attempt(objectOutputStream.writeObject(response))
    _ <- ZIO.attempt(objectInputStream.close())
    _ <- ZIO.attempt(objectOutputStream.close())
    _ <- ZIO.attempt(socket.close())
  } yield ()

  private def process(
      request: TCPRequest,
      objectOutputStream: ObjectOutputStream,
      objectInputStream: ObjectInputStream,
      socket: Socket,
  ) = {
    request.requestType match {
      case PUT =>
        writeResponseAndHandleConnections(TCPResponse(OK), objectOutputStream, objectInputStream, socket) *> createEntryRequestHandler(
          request,
        )
      case GET =>
        getEntryRequestHandler(request).flatMap(writeResponseAndHandleConnections(_, objectOutputStream, objectInputStream, socket))
      case GET_ALL =>
        getAllRequestHandler(request).flatMap(writeResponseAndHandleConnections(_, objectOutputStream, objectInputStream, socket))
    }
  }

  private def createEntryRequestHandler(request: TCPRequest): URIO[Any, TCPResponse] = {
    val result = for {
      state <- clusterState.get
      isAuthorized = state.isLeader || request.resource == state.currentLeader.map(_.path)
      _ <- ZIO.logTrace(s"Creating new entries with key: ${request.entity.map(_.key)}")
      _ <-
        if (isAuthorized) cacheService.put(request.entity, state.isLeader)
        else
          ZIO.logTrace("Creating new entries failed, client is not authorizes to create new entries") *> ZIO.fail(
            UnauthorizedCacheCreateEntryRequest,
          )
    } yield ()

    result.fold(
      error => TCPResponse(Unauthorized),
      _ => TCPResponse(OK),
    )
  }

  private def getEntryRequestHandler(request: TCPRequest): ZIO[Any, Nothing, TCPResponse] = {
    request.key match {
      case Some(key) =>
        cacheService.get(key).map {
          case Some(data) => TCPResponse(OK, List(CacheEntryResponse(key, data)))
          case None       => TCPResponse(OK)
        }
      case None => ZIO.succeed(TCPResponse(BadRequest))
    }
  }

  private def getAllRequestHandler(request: TCPRequest): URIO[Any, TCPResponse] = {
    val result = for {
      isAuthorized <- clusterState.get.map(s => s.isLeader && s.workers.exists(w => request.resource.contains(w.path)))
      entries <-
        if (isAuthorized) cacheService.getAll.map(_.map(r => CacheEntryResponse(r.key, r.value)))
        else ZIO.fail(UnauthorizedCacheCreateEntryRequest)
    } yield entries

    result.fold(
      error => TCPResponse(Unauthorized),
      data => TCPResponse(OK, data),
    )
  }
}

object ZIOTCPServer {
  val live: ZLayer[Ref[ZookeeperClusterState] & CacheService & AppConfig, Nothing, ZIOTCPServer] = ZLayer.fromZIO {
    for {
      appConfig    <- ZIO.service[AppConfig]
      cacheService <- ZIO.service[CacheService]
      clusterState <- ZIO.service[Ref[ZookeeperClusterState]]
    } yield ZIOTCPServer(appConfig, cacheService, clusterState)
  }
}
