package sandbox

import cats.syntax.all.*
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*

import java.nio.charset.StandardCharsets
import scala.scalanative.libc.*
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
                stringToCString(
                  printer.print(result.asJson)
                )
              case Left(error)  =>
                stringToCString(s"""{ "error": "${error.show.replace("\"", "\\\"")}" }""")
            }
          case Left(error) =>
            stringToCString(s"""{ "error": "${error.show.replace("\"", "\\\"")}" }""")
        }
      } catch {
        case error: Throwable =>
          stringToCString(s"""{ "error": "${error.getMessage.replace("\"", "\\\"")}" }""")
//      } finally {
//        System.gc()
      }

  }

  private def interop[In: Decoder]: InteropWithInput[In] = new InteropWithInput[In]

  private def stringToCString(string: String): CString = {
    val bytes  = string.getBytes(StandardCharsets.UTF_8)
    val result = stdlib.malloc(bytes.length + 1)

    bytes.zipWithIndex.foreach { case (byte, index) =>
      !(result + index) = byte
    }
    !(result + string.length) = 0.toByte // Null terminator
    result
  }

  @exported("free_bridge_result")
  def free_bridge_result(str: CString): Unit = {
    stdlib.free(str)
  }

}
