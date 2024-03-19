package com.pinkstack.ta2

import cats.syntax.all.*
import org.http4s.{EntityEncoder, FormDataDecoder}
import org.http4s.FormDataDecoder.*
import org.http4s.multipart.*

import java.io.{File, InputStream}

final case class NewDownload(
  uri: String,
  hidden: Boolean = false
)

object NewDownloadDecoder {
  import org.http4s.FormDataDecoder.*

  /*
  implicit def inputStreamEncoder[A <: InputStream]: EntityEncoder[Eval[A]] =
    sourceEncoder[Byte].contramap { in: Eval[A] =>
      readInputStream[Task](Task.delay(in.value), DefaultChunkSize)
    }

  implicit def sourceEncoder[A](implicit W: EntityEncoder[A]): EntityEncoder[Stream[Task, A]] =
    new EntityEncoder[Stream[Task, A]] {
      override def toEntity(a: Stream[Task, A]): Task[Entity] =
        Task.now(Entity(a.evalMap(W.toEntity).flatMap(_.body)))

      override def headers: Headers =
        W.headers.get(`Transfer-Encoding`) match {
          case Some(transferCoding) if transferCoding.hasChunked =>
            W.headers
          case _ =>
            W.headers.put(`Transfer-Encoding`(TransferCoding.chunked))
        }
    }
  */

  given FormDataDecoder[NewDownload] =
    (
      field[String]("uri"),
      // field[File]("file"),
      field[Boolean]("hidden")
    ).mapN { case (uri, file) => NewDownload(uri, file) }
}
