package com.pinkstack.ta2

object DownloadExtensions:
  extension (download: Download)
    def downloadTitle: String =
      val torrentName  = download.bittorrent.flatMap(_.info.flatMap(_.name))
      val fileInfoName = download.files.headOption.flatMap(_.path)
      (torrentName orElse fileInfoName).getOrElse(download.gid)

    def fields: List[(String, String)] =
      val names  = download.productElementNames.toList
      val values = download.productIterator.toList
      names.zip(values).map {
        case (key, values: Vector[_]) => key -> values.mkString(", ")
        case (key, values: Seq[_])    => key -> values.mkString(", ")
        case (key, values: Array[_])  => key -> values.mkString(", ")
        case (key, value)             =>
          key -> value.toString
      }

    def progress: String =
      (download.completedLength, download.totalLength) match
        case (0, _)                         => "NaN"
        case (completedLength, totalLength) => (completedLength / totalLength * BigInt(100)).toString()+"%"
