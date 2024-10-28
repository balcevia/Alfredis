package com.alfredis.httpclient

import cats.syntax.either.*
import com.alfredis.error.{DomainError, HttpClientDecodingError, HttpClientError}
import io.circe.syntax.*
import io.circe.{Codec, Decoder, Printer, parser}
import sttp.client3.*
import sttp.client3.asynchttpclient.zio.{AsyncHttpClientZioBackend, ZioWebSocketsStreams}
import sttp.model.{StatusCode, Uri}
import zio.{IO, Task, ZLayer}

case class HttpClient(backend: SttpBackend[Task, ZioWebSocketsStreams]) {

  private val jsonContentType = "application/json"

  def getRequest(url: String): RequestT[Identity, String, Any] = quickRequest.get(Uri.unsafeParse(url)).response(asStringAlways)

  def postRequest[T: Codec](url: String, body: T): RequestT[Identity, String, Any] = {
    val serializedBody = body.asJson.printWith(Printer.noSpaces)
    postRequest(url, serializedBody)
  }

  def postRequest(url: String, body: String): RequestT[Identity, String, Any] =
    quickRequest
      .contentType(jsonContentType)
      .post(Uri.unsafeParse(url))
      .body(body)
      .response(asStringAlways)

  private def decodeResponse[ExtResp](
      responseString: String,
  )(implicit
      decoder: Decoder[ExtResp],
  ): Either[DomainError, ExtResp] =
    parser
      .decode(responseString)(using decoder)
      .fold(
        error => HttpClientDecodingError(error.getMessage).asLeft,
        value => value.asRight,
      )

  def callApi[ExtResp](
      request: Request[String, Any],
  )(implicit decoder: Decoder[ExtResp]): IO[DomainError, ExtResp] = {
    request
      .send(backend)
      .map { response =>
        response.contentType match {
          case Some(contentType) if contentType == jsonContentType =>
            decodeResponse[ExtResp](response.body)
          case contentType =>
            HttpClientError(s"Unexpected content type: $contentType, while processing request to ${request.uri}").asLeft
        }
      }
      .mapError(error => HttpClientError(error.getMessage))
      .absolve
  }

  def callApiUnit(request: Request[String, Any]): IO[DomainError, Unit] = {
    request
      .send(backend)
      .map { response =>
        response.code match {
          case StatusCode.Created | StatusCode.Ok => ().asRight
          case statusCode => HttpClientError(s"Request to ${request.uri} failed, unexpected status code $statusCode").asLeft
        }
      }
      .mapError(error => HttpClientError(error.getMessage))
      .absolve
  }

}

object HttpClient {
  val live: ZLayer[Any, Throwable, HttpClient] = ZLayer.fromZIO {
    for {
      backend <- AsyncHttpClientZioBackend()
    } yield HttpClient(backend)
  }
}
