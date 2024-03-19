package com.pinkstack.ta2

import cats.effect.{IO, Resource}
import org.http4s.client.Client
// import org.http4s.ember.client.EmberClientBuilder
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import io.circe.*
import org.http4s.headers.Authorization
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec._
import scala.concurrent.duration._

trait Aria2Client {
  def tellActive(keys: Vector[String] = Vector.empty): IO[Vector[Download]]
  def tellWaiting(offset: Int, num: Int): IO[Vector[Download]]
  def tellStopped(offset: Int, num: Int, keys: Vector[String] = Vector.empty): IO[Vector[Download]]
  def tellStatus(gid: GID): IO[Download]

  def addUri(uri: String): IO[GID]
  def remove(gid: GID): IO[GID]
  def removeDownloadResult(gid: GID): IO[GID]

  /*
  def forceRemove(gid: GID): IO[GID]
  def pauseRemove(gid: GID): IO[GID]
  def forcePause(gid: GID): IO[GID]
  def unpause(gid: GID): IO[GID]
   */
}

final class Aria2ClientImpl private (
  config: Config,
  client: Client[IO]
) extends Aria2Client:
  private def buildRPCCall(method: String, that: Option[Json] = None): Request[IO] =
    val headers = Headers(Authorization(BasicCredentials(config.aria2Username, config.aria2Password)))
    val entity  = Json
      .fromFields(
        Seq(
          "jsonrpc" -> Json.fromString("2.0"),
          "id"      -> Json.fromString("qwer"),
          "method"  -> Json.fromString(method)
        )
      )
      .deepMerge(that.getOrElse(Json.obj()))

    Request[IO](Method.POST, config.aria2Uri / "jsonrpc").withEntity(entity).withHeaders(headers)

  private def defineRPC[Out](method: String, params: Json*)(using decoder: io.circe.Decoder[Out]): IO[Out] = {
    val call = buildRPCCall(
      method,
      Some(
        if params.isEmpty then Json.obj()
        else Json.obj("params" -> Json.arr(params*))
      )
    )

    client
      .expect[RPCResult[Out]](call)
      .flatMap {
        case RPCResult(_, _, Some(result: Out), _) => IO.pure(result)
        case RPCResult(_, _, _, Some(error)) => IO.raiseError(new RuntimeException(s"Failed with ${error.message}"))
        case _                               => IO.raiseError(new RuntimeException(s"Strange failure"))
      }
  }

  override def tellActive(keys: Vector[String]): IO[Vector[Download]] =
    defineRPC[Vector[Download]]("aria2.tellActive")

  override def tellWaiting(offset: Int, num: Int): IO[Vector[Download]] =
    defineRPC[Vector[Download]]("aria2.tellWaiting", Json.fromInt(offset), Json.fromInt(num))

  override def tellStopped(offset: Int, num: Int, keys: Vector[GID]): IO[Vector[Download]] =
    defineRPC[Vector[Download]]("aria2.tellStopped", Json.fromInt(offset), Json.fromInt(num))

  override def tellStatus(gid: GID): IO[Download] =
    defineRPC[Download]("aria2.tellStatus", Json.fromString(gid))

  override def addUri(uri: GID): IO[GID] =
    defineRPC[GID]("aria2.addUri", Json.arr(Json.fromString(uri)))

  override def remove(gid: GID): IO[GID] =
    defineRPC[GID]("aria2.remove", Json.fromString(gid))

  override def removeDownloadResult(gid: GID): IO[GID] =
    defineRPC[GID]("aria2.removeDownloadResult", Json.fromString(gid))

object Aria2ClientImpl:
  def make(config: Config, client: Client[IO]): Aria2Client = new Aria2ClientImpl(config, client)

object Aria2Client:
  given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  def resource(config: Config): Resource[IO, Aria2Client] =
    for
      client      <- BlazeClientBuilder[IO].withoutSslContext
        .withConnectTimeout(3.seconds)
        .withRequestTimeout(3.seconds)
        .withRetries(2)
        .resource
      aria2client <- Resource.pure(Aria2ClientImpl.make(config, client))
    yield aria2client
