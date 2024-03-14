package com.pinkstack.ta2

type GID = String

final case class FileUri(status: String, uri: String)

final case class FileInfo(
  completedLength: BigInt,
  index: Int,
  length: BigInt,
  // selected: Option[Boolean],
  uris: Array[FileUri],
  path: Option[String]
)

final case class Bittorrent(
  mode: Option[String] = None,
  info: Option[BittorrentInfo] = None
)

final case class BittorrentInfo(name: Option[String] = None)

final case class Download(
  gid: GID,
  dir: String,
  infoHash: Option[String],
  connections: Int,
  status: String,
  numSeeders: Option[BigInt],
  numPieces: BigInt,
  pieceLength: BigInt,
  seeder: Option[String], // TODO: Should be boolean?
  totalLength: BigInt,
  uploadLength: BigInt,
  uploadSpeed: Int,
  downloadSpeed: Int,
  completedLength: BigInt,
  following: Option[String],
  bittorrent: Option[Bittorrent],
  files: Array[FileInfo]
)

final case class RPCResult[T](
  id: String,
  jsonrpc: String,
  result: Option[T],
  error: Option[RPCError]
)

final case class RPCError(code: Int, message: String)
