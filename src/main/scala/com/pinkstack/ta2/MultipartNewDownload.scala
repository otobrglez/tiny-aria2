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
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import scodec.bits.ByteVector
import fs2.text
import fs2.text.utf8

import java.util.Base64
import scala.reflect.ClassTag

enum Field(val fieldName: String):
  case Value[T: ClassTag](override val fieldName: String, value: T) extends Field(fieldName)
  case UnknownFileType(override val fieldName: String)              extends Field(fieldName)

object MultipartNewDownload:
  import Field.*
  given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]
  private val logger                     = loggerFactory.getLogger

  private def parseAsTorrent(fieldName: String, part: Part[IO]): IO[Value[String]] = for
    bodyAsVector <- part.body.compile.toVector.map(ByteVector(_))
    bodyAsBase64 <- IO(Base64.getEncoder.encode(bodyAsVector.toArray))
  yield Value(fieldName, new String(bodyAsBase64))

  private def parseAsString(fieldName: String, part: Part[IO]): IO[Value[String]] =
    for rawContent <- part.body.through(utf8.decode).compile.foldMonoid
    yield Value(fieldName, rawContent)

  private def processFields(multipart: Multipart[IO]): IO[Vector[Field]] =
    multipart.parts.flatMap { part =>
      part.name.map { fieldName =>
        part.headers.get(CIString.apply("Content-Type")) match {
          case Some(NonEmptyList(Header.Raw(_, "application/x-bittorrent"), _)) => parseAsTorrent(fieldName, part)
          case Some(NonEmptyList(Header.Raw(_, "application/octet-stream"), _)) => IO.pure(UnknownFileType(fieldName))
          case None                                                             => parseAsString(fieldName, part)
        }
      }
    }.sequence

  def process(multipart: Multipart[IO]): IO[NewDownload] = for {
    fields <- processFields(multipart)

    uri <- IO(fields.find(_.fieldName == "uri")).map(_.flatMap {
      case value: Value[String] =>
        if value.value.trim.isBlank then None
        else Uri.fromString(value.value).toOption
      case _                    => None
    })

    file <- IO(fields.find(_.fieldName == "file")).map(_.flatMap {
      case value: Value[String] => Some(value.value)
      case _                    => None
    })
  } yield NewDownload(uri, file)
