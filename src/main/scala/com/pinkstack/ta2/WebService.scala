package com.pinkstack.ta2

import cats.data.Kleisli
import cats.effect.{IO, Resource}
import com.pinkstack.ta2.layout.Layout
import org.http4s.Charset.`UTF-8`
import org.http4s.FormDataDecoder.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`
import org.http4s.implicits.*
import org.http4s.multipart.*
import org.http4s.server.Server
import org.http4s.*
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import scalatags.Text
import scalatags.Text.all.doctype

final case class WebService private (config: Config, client: Aria2Client) {
  given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]
  private val logger                     = loggerFactory.getLogger

  given scalaTagsEncoder: EntityEncoder[IO, doctype] =
    EntityEncoder.stringEncoder
      .contramap[doctype](_.render)
      .withContentType(`Content-Type`(MediaType.text.html, `UTF-8`))

  private def renderDownloads(title: String, downloads: Vector[Download]): IO[Text.all.doctype] =
    IO.pure(Layout.layout(title)(Some(Layout.downloads(downloads))))

  private def renderError(throwable: Throwable): IO[Text.all.doctype] =
    logger.error(throwable)(s"Caught error - ${throwable.getMessage}") *>
      IO.pure(
        Layout.layout(s"Error - ${throwable.getMessage}")(
          Some(Layout.error(throwable))
        )
      )

  private def renderInfo(message: String): IO[Text.all.doctype] =
    IO.pure(Layout.layout("Ok")(Some(Layout.info(message))))

  private def renderNewDownload(maybeUri: Option[String]) =
    IO.pure(Layout.layout("New Download")(Some(Layout.newDownload(maybeUri))))

  private def renderStatus(download: Download) =
    IO.pure(
      Layout.layout(s"Status of ${download.gid}")(
        Some(
          Layout.status(download)
        )
      )
    )

  val logErrorAndNullify: Throwable => IO[Vector[Download]] =
    err => logger.error(err)(s"Client failed with - ${err}") *> IO.pure(Vector.empty)

  private val routes = HttpRoutes
    .of[IO] {
      case GET -> Root =>
        client
          .tellActive()
          .flatMap(renderDownloads("Active", _))
          .handleErrorWith(renderError)
          .flatMap(Ok(_))

      case GET -> Root / "waiting" =>
        client
          .tellWaiting(0, 100)
          .flatMap(renderDownloads("Waiting", _))
          .handleErrorWith(renderError)
          .flatMap(Ok(_))

      case GET -> Root / "stopped" =>
        client
          .tellStopped(0, 100)
          .flatMap(renderDownloads("Stopped", _))
          .handleErrorWith(renderError)
          .flatMap(Ok(_))

      case GET -> Root / "status" / gid =>
        client
          .tellStatus(gid)
          .flatMap(renderStatus)
          .handleErrorWith(renderError)
          .flatMap(Ok(_))

      case GET -> Root / "remove" / gid =>
        client
          .remove(gid)
          .flatMap(gid => renderInfo(s"Deleted ${gid}"))
          .handleErrorWith(renderError)
          .flatMap(Ok(_))

      case GET -> Root / "removeDownloadResult" / gid =>
        client
          .removeDownloadResult(gid)
          .flatMap(gid => renderInfo(s"Deleted ${gid}"))
          .handleErrorWith(renderError)
          .flatMap(Ok(_))

      case GET -> Root / "new-download" =>
        for
          newDownload <- renderNewDownload(None)
          response    <- Ok(newDownload)
        yield response

      case req @ POST -> Root / "new-download" =>
        {
          for {
            newDownload <- req.as[Multipart[IO]].flatMap(MultipartNewDownload.process)
            gid         <- newDownload match {
              case NewDownload(Some(uri), _)  => client.addUri(uri.toString)
              case NewDownload(_, Some(file)) => client.addTorrent(file)
              case NewDownload(None, None)    =>
                IO.raiseError(new RuntimeException("Please add either an URI/Magnet or upload Torrent file"))
            }
            result      <- renderInfo(s"Successfully added new download! GID: $gid")
          } yield result
        }.handleErrorWith(renderError).flatMap(Ok(_))
    }

  val httpApp: Kleisli[IO, Request[IO], Response[IO]] = routes.orNotFound
}

object WebService:
  given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  def resource(config: Config, client: Aria2Client): Resource[IO, Server] =
    for
      appService <- Resource.pure(apply(config, client))
      server     <- BlazeServerBuilder[IO]
        .bindHttp(config.port.toString.toInt, "0.0.0.0")
        .withHttpApp(appService.httpApp)
        .withBanner(Seq.empty)
        .resource
    yield server
