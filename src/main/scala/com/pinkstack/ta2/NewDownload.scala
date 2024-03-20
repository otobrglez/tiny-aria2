package com.pinkstack.ta2

import org.http4s.Uri

final case class NewDownload(
  uri: Option[Uri] = None,
  file: Option[String] = None
)
