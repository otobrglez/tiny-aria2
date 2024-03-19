package com.pinkstack.ta2

import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp, Resource, ResourceApp}
import com.comcast.ip4s.{ipv4, Port}
import org.http4s.{EntityEncoder, FormDataDecoder, HttpRoutes, MediaType, Request, Response}
import org.http4s.FormDataDecoder.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import com.comcast.ip4s.*
import com.pinkstack.ta2.layout.Layout
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.headers.`Content-Type`
import org.http4s.server.Server
import scalatags.Text.all.doctype
import org.http4s.Charset.`UTF-8`
import scalatags.Text
import cats.data.Validated.*
import cats.syntax.all.*
import org.http4s.multipart.*

final case class WebService private (config: Config, client: Aria2Client) {
  import NewDownloadDecoder.given
  import MultipartProcessorResult.*

  given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]
  private val logger                     = loggerFactory.getLogger

  given scalaTagsEncoder: EntityEncoder[IO, doctype] =
    EntityEncoder.stringEncoder
      .contramap[doctype](_.render)
      .withContentType(`Content-Type`(MediaType.text.html, `UTF-8`))

  def renderDownloads(title: String, downloads: Vector[Download]): IO[Text.all.doctype] =
    IO.pure(Layout.layout(title)(Some(Layout.downloads(downloads))))

  def renderError(throwable: Throwable): IO[Text.all.doctype] =
    logger.error(throwable)(s"Caught error - ${throwable.getMessage}") *>
      IO.pure(
        Layout.layout(s"Error - ${throwable.getMessage}")(
          Some(Layout.error(throwable))
        )
      )

  def renderInfo(message: String): IO[Text.all.doctype] =
    IO.pure(Layout.layout("Ok")(Some(Layout.info(message))))

  def renderNewDownload(maybeUri: Option[String]) =
    IO.pure(Layout.layout("New Download")(Some(Layout.newDownload(maybeUri))))

  def renderStatus(download: Download) =
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
        for
          regularForm <- req.as[NewDownload].handleErrorWith(th => logger.warn(th)("invalid form") *> IO.unit)
          _           <- IO.println(regularForm)

          multipartProcessorResult <- req.as[Multipart[IO]].flatMap(MultipartProcessor.process)
          response                 <- multipartProcessorResult match
            case InvalidFileKind(kind) =>
              Ok(renderError(new RuntimeException(s"Unsupported file kind - $kind")))

            case Base64EncodedFile(file) =>
              client
                .addTorrent(file)
                .flatMap(gid => renderInfo(s"Added new download. GID: $gid"))
                .handleErrorWith(renderError)
                .flatMap(Ok(_))

            case EmptyForm() =>
              Ok("TODO: Empty form but field uri might be set...")
            /*
              req
                .as[NewDownload]
                .flatMap { newDownload =>
                  client
                    .addUri(newDownload.uri)
                    .flatMap(gid => renderInfo(s"Added new download. GID: $gid"))
                }
                .handleErrorWith(renderError)
                .flatMap(Ok(_)) */
        yield response
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
    /*
      server     <- BlazeServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(config.port)
        .withHttpApp(appService.httpApp)
        .build */
    yield server
