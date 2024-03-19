package com.pinkstack.ta2

import cats.data.{NonEmptyList, OptionT}
import cats.implicits.*
import cats.effect.IO
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.headers.*
import org.http4s.multipart.{Multipart, Part}
import org.http4s.MediaType.*
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import scodec.bits.ByteVector

import scala.jdk.CollectionConverters.*
import java.util.Base64

enum MultipartProcessorResult:
  case Base64EncodedFile(file: String)
  case InvalidFileKind(kind: String)
  case EmptyForm()

object MultipartProcessor {
  import MultipartProcessorResult.*
  given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]
  private val logger                     = loggerFactory.getLogger

  private def parseAsTorrent(part: Part[IO]): IO[Base64EncodedFile] = for {
    bodyAsVector <- part.body.compile.toVector.map(ByteVector(_))
    bodyAsBase64 <- IO(Base64.getEncoder.encode(bodyAsVector.toArray))
  } yield Base64EncodedFile(new String(bodyAsBase64))

  private def parseAsEmptyForm(part: Part[IO]): IO[EmptyForm] =
    IO.println("empty form") *> IO.pure(EmptyForm())

  def process(multipart: Multipart[IO]): IO[MultipartProcessorResult] =
    val pom: IO[Vector[Option[MultipartProcessorResult]]] = multipart.parts.map { part =>
      part.headers.get(CIString.apply("Content-Type")) match {
        case Some(NonEmptyList(Header.Raw(_, "application/x-bittorrent"), _)) => parseAsTorrent(part).map(Some(_))
        case Some(NonEmptyList(Header.Raw(_, "application/octet-stream"), _)) => parseAsEmptyForm(part).map(Some(_))
        case Some(NonEmptyList(Header.Raw(_, kind), _))                       =>
          logger.warn(s"Unsupported header kind - $kind") *> IO.pure(InvalidFileKind(kind).some)
        case None                                                             => IO.pure(None)
      }
    }.sequence

    pom.flatMap { v =>
      IO.fromOption(v.collectFirst { case Some(v) => v })(new RuntimeException("Failed parsing payload."))
    }

}
