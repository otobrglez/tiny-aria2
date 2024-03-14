package com.pinkstack.ta2

import cats.effect.{IO, Resource, ResourceApp}
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

object ServerApp extends ResourceApp.Forever:
  given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]
  private val logger                     = loggerFactory.getLogger

  override def run(args: List[String]): Resource[IO, Unit] = for
    _           <- logger.info("Booting...").toResource
    config      <- Config.load().flatTap(config => logger.info(s"Using Aria2 endpoint ${config.aria2Uri}")).toResource
    aria2client <- Aria2Client.resource(config)
    _           <- WebService.resource(config, aria2client)
  yield ()
