package sandbox

import io.circe.*

case class SampleOutput(
  data: JsonObject,
  now: String,
) derives Encoder, Decoder
