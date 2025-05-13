package sandbox

import java.time.ZoneId

case class LocalTimeZone(
  zoneName: String
) {

  lazy val zoneId: ZoneId = ZoneId.of(zoneName)

}
