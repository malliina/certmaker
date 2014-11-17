package com.mle.cert

import java.nio.file.{Path, Paths}

import com.mle.json.JsonFormats.SimpleFormat
import play.api.libs.json.Json

/**
 * @author Michael
 */
case class Meta(description: String, path: Path)

case object Meta {
  implicit val pathJson = new SimpleFormat[Path](s => Paths get s)
  implicit val json = Json.format[Meta]
}