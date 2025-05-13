package sandbox

import io.circe.*

case class SampleInput(
  data: JsonObject,
  depth: Int,
  systemTimezone: String,
) derives Encoder, Decoder
