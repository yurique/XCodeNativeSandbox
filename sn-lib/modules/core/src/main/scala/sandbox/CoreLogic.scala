package sandbox

import io.circe.Json
import io.circe.JsonObject
import io.circe.syntax.*

import java.time.Instant
import java.time.LocalDateTime
import scala.util.Random

object CoreLogic {

  def sampleLogic(input: SampleInput)(using ltz: LocalTimeZone): SampleOutput = {
    val randomStuff = generateSubTree(input.depth)
    SampleOutput(
      data = input.data.deepMerge(randomStuff),
      now = LocalDateTime.now(ltz.zoneId).toString
    )
  }

  private val approxYearInMillis = 365 * 24 * 60 * 60 * 1000

  private def generateSubTree(depthRemaining: Int)(using ltz: LocalTimeZone): JsonObject = {
    JsonObject(
      "a"        -> Random.nextLong().asJson,
      "b"        -> Random.nextLong().asJson,
      "time"     -> LocalDateTime.ofInstant(Instant.now().plusMillis(Random.nextLong(approxYearInMillis) - approxYearInMillis / 2), ltz.zoneId).toString.asJson,
      "children" -> Option.when(depthRemaining > 0) {
        Json.arr(
          (1 to 2).map { _ =>
            generateSubTree(depthRemaining - 1).asJson
          }.toList*
        )
      }.asJson
    )

  }

}
