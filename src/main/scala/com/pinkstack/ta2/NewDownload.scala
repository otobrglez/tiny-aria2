package com.pinkstack.ta2

import cats.syntax.all.*
import org.http4s.FormDataDecoder

final case class NewDownload(
  uri: String,
  hidden: Boolean
)

object NewDownloadDecoder {
  import org.http4s.FormDataDecoder.*

  given FormDataDecoder[NewDownload] =
    (
      field[String]("uri"),
      field[Boolean]("hidden")
    ).mapN { case (a, b) => NewDownload(a, b) }
}
