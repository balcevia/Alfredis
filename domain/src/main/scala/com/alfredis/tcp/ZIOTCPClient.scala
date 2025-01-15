package com.alfredis.tcp

import com.alfredis.api.model.CacheEntryRequest
import com.alfredis.tcp.TCPRequestType.GET
import zio.*

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.Socket

case class ZIOTCPClient(host: String, port: Int) {
  def put(key: String, value: Array[Byte]): ZIO[Any, Throwable, TCPResponse] = {
    val request = TCPRequest(
      requestType = TCPRequestType.PUT,
      key = None,
      entity = List(CacheEntryRequest(key, value)),
      resource = None,
    )
    makeRequest(request)
  }

  def get(key: String): ZIO[Any, Throwable, Option[Array[Byte]]] = {
    val request = TCPRequest(
      requestType = TCPRequestType.GET,
      key = Some(key),
      entity = Nil,
      resource = None,
    )
    makeRequest(request).map(_.data.headOption.map(_.value))
  }

  def getAll: ZIO[Any, Throwable, List[(String, Array[Byte])]] = {
    val request = TCPRequest(
      requestType = TCPRequestType.GET_ALL,
      key = None,
      entity = Nil,
      resource = None,
    )
    makeRequest(request).map(_.data.map(e => e.key -> e.value))
  }

  private def makeRequest(entity: TCPRequest): ZIO[Any, Throwable, TCPResponse] = for {
    socket             <- ZIO.attempt(new Socket(host, port))
    objectOutputStream <- ZIO.attempt(new ObjectOutputStream(socket.getOutputStream))
    objectInputStream  <- ZIO.attempt(new ObjectInputStream(socket.getInputStream))
    _                  <- ZIO.attempt(objectOutputStream.writeObject(entity))
    response           <- ZIO.attempt(objectInputStream.readObject().asInstanceOf[TCPResponse])
    _                  <- ZIO.logInfo(s"Got response: $response")
    _                  <- ZIO.attempt(objectInputStream.close())
    _                  <- ZIO.attempt(objectOutputStream.close())
    _                  <- ZIO.attempt(socket.close())
  } yield response
}

object ZIOTCPClient {
  def live(host: String, port: Int): ZLayer[Any, Nothing, ZIOTCPClient] = ZLayer.fromZIO(ZIO.succeed(ZIOTCPClient(host, port)))
}
