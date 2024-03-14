package com.pinkstack.ta2

import cats.effect.IO
import IO.{fromEither, fromOption}
import cats.effect.std.Env
import com.comcast.ip4s.Port
import org.http4s.Uri

final case class Config(
  aria2Uri: Uri,
  aria2Username: String,
  aria2Password: String,
  port: Port
)

final case class MissingEnv(message: String) extends Throwable(message)
object MissingEnv:
  def make(message: String): MissingEnv = apply(message)

object Config:
  private def readEnvString(key: String): IO[String] =
    Env[IO]
      .get(key)
      .flatMap(fromOption(_)(MissingEnv.make(s"Missing env variable \"${key}\"")))

  def load(): IO[Config] =
    for
      aria2Uri      <- readEnvString("ARIA2_URI").flatMap(raw => fromEither(Uri.fromString(raw)))
      aria2Username <- readEnvString("ARIA2_USERNAME")
      aria2Password <- readEnvString("ARIA2_PASSWORD")
      port          <- readEnvString("PORT")
        .flatMap(port => fromOption(port.toIntOption)(new RuntimeException("Missing PORT")))
        .flatMap(port => fromOption(Port.fromInt(port))(new RuntimeException("Failed getting PORT")))
    yield Config(
      aria2Uri,
      aria2Username,
      aria2Password,
      port
    )
