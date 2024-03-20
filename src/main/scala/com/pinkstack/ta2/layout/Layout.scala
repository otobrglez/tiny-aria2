package com.pinkstack.ta2.layout

import com.pinkstack.ta2.{Download, DownloadExtensions}
import scalatags.Text
import scalatags.Text.all.*
import scalatags.Text.tags2.{style as styleTag, title}
import scalatags.Text.all.doctype

object Layout:
  import DownloadExtensions.*

  private val cssStyle: String =
    """
      |body,td,th,p,input,label {
      | font-family:Roboto, 'Robot Sans', 'Roboto-Sans', sans-serif; font-size:14pt}
      |a { color: #189AB4; }
      |a:hover { color: #05445E; }
      |html,body { margin:0; padding: 10pt; }
      |.header .logo { display: inline-block; margin-right:20px; }
      |.header .logo .tiny { font-weight: 700; }
      |.header { line-height: 16pt; margin-bottom: 30px }
      |.header a { margin-right: 10px; }
      |.error { color: red; }
      |.downloads { display:block; clear:both; float:none; }
      |.downloads .download { display: block; clear: both; float:none; position: relative; margin-bottom: 10px; }
      |.downloads .download .tools a { margin-right: 5px; color: #666; font-size:small; }
      |.download .raw-title .progress { margin-left: 15px; color: #333 }
      |.download-status .fields .field { display:block; float: none; clear: both; position: relative; margin-bottom: 10px }
      |.download-status .fields .name { font-weight:bold }
      |.new-download input { min-width:200px; }
      |.new-download span { margin-right: 10px }
      |.new-download .input-wrap { margin-bottom: 10px }
      |""".stripMargin

  def layout[T <: String](
    rawTitle: String
  )(
    maybeContent: Option[Text.TypedTag[T]] = None
  ): Text.all.doctype =
    doctype("html")(
      html(
        head(
          title(rawTitle),
          meta(charset:="UTF8"),
          meta(name:="viewport", content:="width=device-width, initial-scale=1"),
          styleTag(cssStyle)
        ),
        body(
          div(
            cls := "header",
            div(cls := "logo", span(cls := "tiny", "Tiny"), "Aria2"),
            a(href  := "/", "Active"),
            a(href  := "/stopped", "Stopped"),
            a(href  := "/waiting", "Waiting"),
            a(href  := "/new-download", "New download")
          ),
          maybeContent.fold(div(""))(content => div(cls := "content", content))
        )
      )
    )

  def error(throwable: Throwable): Text.TypedTag[String] =
    div(cls := "error", div(cls := "message", throwable.getMessage))

  def downloads(downloads: Vector[Download]): Text.TypedTag[String] =
    div(
      cls := "downloads",
      if downloads.isEmpty then {
        div(cls := "no-downloads", "No downloads here.")
      } else
        downloads.map { download =>
          div(
            cls              := "download",
            attr("data-gid") := download.gid,
            div(
              cls := "title",
              div(
                cls := "raw-title",
                download.downloadTitle,
                span(cls := "progress", download.status + " " + download.progress)
              )
            ),
            div(
              cls := "tools",
              a(href := s"/status/${download.gid}", "Status"),
              a(href := s"/remove/${download.gid}", "Remove"),
              a(href := s"/removeDownloadResult/${download.gid}", "Remove result")
            )
          )
        }
    )

  def info(message: String): Text.TypedTag[String] =
    div(cls := "info", div(cls := "message", message))

  def newDownload(maybeUri: Option[String]): Text.TypedTag[String] =
    div(
      cls := "new-download",
      form(
        action  := "/new-download",
        method  := "post",
        enctype := "multipart/form-data",
        div(
          cls        := "input-wrap",
          label(span("URI or Magnet"), input(`type` := "text", name := "uri", value := maybeUri.getOrElse("")))
        ),
        div(
          cls        := "input-wrap",
          label(span("Torrent file"), input(`type` := "file", name := "file", accept:=".torrent,application/x-bittorrent"))
        ),
        br(),
        div(cls      := "input-wrap", input(`type` := "submit", value := "Add URI / Magnet")),
        br(),
        input(`type` := "hidden", value := "true", name := "hidden")
      )
    )

  def status(download: Download): Text.TypedTag[String] =
    div(
      cls := "download-status",
      h1(download.downloadTitle),
      div(
        cls := "fields",
        download.fields.map { case (name, value) =>
          div(cls := "field", div(cls := "name", name), div(cls := "value", value))
        }
      )
    )
