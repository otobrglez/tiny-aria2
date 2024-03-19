package com.pinkstack.ta2

import cats.syntax.all.*
import org.http4s.FormDataDecoder
import org.http4s.FormDataDecoder._
import org.http4s.multipart._

import java.io.File

final case class NewDownload(
  uri: String,
  hidden: Boolean = false
)

object NewDownloadDecoder {
  import org.http4s.FormDataDecoder.*

  given FormDataDecoder[NewDownload] =
    (
      field[String]("uri"),
      // field[File]("file"),
      field[Boolean]("hidden")
    ).mapN { case (uri, file) => NewDownload(uri, file) }
}
