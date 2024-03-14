import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import Dependencies.*

ThisBuild / version            := "0.0.4"
ThisBuild / scalaVersion       := "3.4.0"
ThisBuild / evictionErrorLevel := Level.Info

lazy val root = (project in file("."))
  .enablePlugins(JavaServerAppPackaging, DockerPlugin)
  .settings(name := "tiny-aria2")
  .settings(
    libraryDependencies ++= {
      catsAndFriends ++ fs2 ++ circe ++ scalaTags ++ logging ++ http4s
    },
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      "-Yretain-trees",
      "-Xmax-inlines:100"
    )
  )
  .settings(
    assembly / assemblyJarName := "tiny-aria2.jar",
    dockerExposedPorts         := Seq(4447),
    dockerExposedUdpPorts      := Seq.empty[Int],
    dockerUsername             := Some("pinkstack"),
    dockerUpdateLatest         := true,
    dockerBaseImage            := "azul/zulu-openjdk-alpine:21-latest",
    packageName                := "tiny-aria2",
    dockerCommands             := dockerCommands.value.flatMap {
      case add @ Cmd("RUN", args @ _*) if args.contains("id") =>
        List(
          Cmd("LABEL", "maintainer Oto Brglez <otobrglez@gmail.com>"),
          Cmd("LABEL", "org.opencontainers.image.url https://github.com/otobrglez/tiny-aria2"),
          Cmd("LABEL", "org.opencontainers.image.source https://github.com/otobrglez/tiny-aria2"),
          Cmd("RUN", "apk add --no-cache bash jq curl"),
          Cmd("ENV", "SBT_VERSION", sbtVersion.value),
          Cmd("ENV", "SCALA_VERSION", scalaVersion.value),
          Cmd("ENV", "TINY_ARIA2_VERSION", version.value),
          add
        )
      case other                                              => List(other)
    }
  )
