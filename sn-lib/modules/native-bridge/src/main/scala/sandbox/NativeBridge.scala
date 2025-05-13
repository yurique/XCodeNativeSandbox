package sandbox

import cats.syntax.all.*
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*

import java.nio.charset.StandardCharsets
import scala.scalanative.libc.*
import scala.scalanative.runtime.Intrinsics.castIntToRawSizeUnsigned
import scala.scalanative.runtime.Intrinsics.unsignedOf
import scala.scalanative.runtime.ffi
import scala.scalanative.unsafe.*

object NativeBridge {

  private val printer = Printer.noSpaces.copy(dropNullValues = true)

  println(s"GC_INITIAL_HEAP_SIZE: ${System.getenv("GC_INITIAL_HEAP_SIZE")}")
  println(s"GC_MAXIMUM_HEAP_SIZE: ${System.getenv("GC_MAXIMUM_HEAP_SIZE")}")

  // ------------------

  @exported("native_sandbox_test")
  def native_sandbox_test(c_input: CString): CString =
    interop[SampleInput].runWithRaw(c_input)(_.systemTimezone) { _ => input =>
      CoreLogic.sampleLogic(input).asJson
    }

  private class InteropWithInput[In: Decoder] {

    def run[Out: Encoder](c_input: CString)(systemTimezone: In => String)(body: LocalTimeZone ?=> In => Out): CString =
      runWithRaw[Out](c_input)(systemTimezone) { _ => in => body(in) }

    def runWithRaw[Out: Encoder](c_input: CString)(systemTimezone: In => String)(body: LocalTimeZone ?=> Json => In => Out): CString =
      try {
        val inputString = fromCString(c_input)
        parse(inputString) match {
          case Right(json) =>
            json.as[In] match {
              case Right(input) =>
                val ltz    = LocalTimeZone(systemTimezone(input))
                val result = body(using ltz)(json)(input)
                toCString(
                  printer.print(result.asJson)
                )
              case Left(error)  =>
                toCString(
                  JsonObject(
                    "snError"     -> "DecodingFailure".asJson,
                    "details"     -> error.show.asJson,
                    "parsedInput" -> json,
                  ).asJson.noSpaces
                )

            }
          case Left(error) =>
            toCString(
              JsonObject(
                "snError" -> "ParsingFailure".asJson,
                "details" -> error.show.asJson,
                "input"   -> inputString.asJson,
              ).asJson.noSpaces
            )
        }
      } catch {
        case error: Throwable =>
          toCString(
            JsonObject(
              "snError" -> error.getClass.getName.asJson,
              "details" -> error.getMessage.asJson,
            ).asJson.noSpaces
          )
//      } finally {
//        System.gc()
      }

  }

  private def interop[In: Decoder]: InteropWithInput[In] = new InteropWithInput[In]

  // copy from std toCString, but without Zone
  private def toCString(str: String): CString = {
    if str == null then {
      null
    } else {
      val bytes = str.getBytes(StandardCharsets.UTF_8)
      if bytes.nonEmpty then {
        val len     = bytes.length
        val rawSize = castIntToRawSizeUnsigned(len + 1)
        val size    = unsignedOf(rawSize)

        val cstr = stdlib.malloc(size)
        val _    = ffi.memcpy(cstr, bytes.at(0), size)
        cstr(len) = 0.toByte

        cstr
      } else c""
    }
  }

  @exported("free_bridge_result")
  def free_bridge_result(str: CString): Unit = {
    stdlib.free(str)
  }

}
