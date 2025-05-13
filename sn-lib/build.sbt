import org.typelevel.scalacoptions.ScalacOptions

import scala.sys.process.Process

import scala.scalanative.build.*

ThisBuild / scalaVersion     := "3.6.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.yurique"
ThisBuild / organizationName := "yurique"

lazy val libTzdb = crossProject(NativePlatform)
  .in(file("modules/tzdb"))
  .settings(
    name := "tzdb",
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.6.0",
    ),
    tpolecatExcludeOptions ++= Set(
      ScalacOptions.warnNonUnitStatement,
      ScalacOptions.deprecation,
    )
  )
  .nativeConfigure(
    _.enablePlugins(TzdbPlugin).settings(
      dbVersion    := TzdbPlugin.Version("2024a"),
      tzdbPlatform := TzdbPlugin.Platform.Native
    )
  )

lazy val core = crossProject(NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/core"))
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core"    % "2.13.0",
      "io.circe"      %%% "circe-core"   % "0.14.10",
      "io.circe"      %%% "circe-parser" % "0.14.10"
    ),
    tpolecatExcludeOptions ++= Set(
      ScalacOptions.warnNonUnitStatement,
      ScalacOptions.deprecation,
    )
  )
  .dependsOn(libTzdb)

lazy val nativeBridge =
  project
    .in(file("modules/native-bridge"))
    .enablePlugins(ScalaNativePlugin)
    .settings(
      libraryDependencies ++= Seq(
        "io.circe" %%% "circe-core"   % "0.14.10",
        "io.circe" %%% "circe-parser" % "0.14.10"
      ),
    )
    .dependsOn(core.native)

lazy val commonNativeOptions = Seq(
//  "-I" + sys.process.Process("brew --prefix bdw-gc").!!.trim + "/include",
//  "-L" + sys.process.Process("brew --prefix bdw-gc").!!.trim + "/lib"
)

lazy val nativeBridgeIos =
  project
    .in(file("modules/native-bridge-ios"))
    .enablePlugins(ScalaNativePlugin)
    .settings(
      nativeConfig ~= { c =>
        c.withBuildTarget(BuildTarget.libraryStatic)
          .withLTO(LTO.none) // thin
          .withMode(Mode.releaseFast)
          .withGC(GC.immix)
          .withMultithreading(false)
          .withTargetTriple(
            "arm64-apple-ios"
          )
          .withCompileOptions(
            c.compileOptions ++
              Seq(
                "-isysroot",
                sys.process.Process("xcrun --sdk iphoneos --show-sdk-path").!!.trim
//              "-mios-version-min=14.0" // Match your deployment target
              ) ++
              commonNativeOptions
          )
          .withLinkingOptions(
            c.linkingOptions ++
              Seq(
                "-isysroot",
                sys.process.Process("xcrun --sdk iphoneos --show-sdk-path").!!.trim
//              "-mios-version-min=14.0" // Match your deployment target
              ) ++
              commonNativeOptions
          )
      }
    )
    .dependsOn(nativeBridge)

lazy val nativeBridgeSimulator =
  project
    .in(file("modules/native-bridge-simulator"))
    .enablePlugins(ScalaNativePlugin)
    .settings(
      nativeConfig ~= { c =>
        c.withBuildTarget(BuildTarget.libraryStatic)
          .withLTO(LTO.none) // thin
          .withMode(Mode.releaseFast)
          .withGC(GC.immix)
          .withMultithreading(false)
          .withTargetTriple(
            "arm64-apple-ios-simulator"
          )
          .withCompileOptions(
            c.compileOptions ++
              Seq(
                "-isysroot",
                sys.process.Process("xcrun --sdk iphonesimulator --show-sdk-path").!!.trim,
                "-mios-version-min=17.0" // Match your deployment target
              ) ++
              commonNativeOptions
          )
          .withLinkingOptions(
            c.linkingOptions ++
              Seq(
                "-isysroot",
                sys.process.Process("xcrun --sdk iphonesimulator --show-sdk-path").!!.trim,
                "-mios-version-min=17.0" // Match your deployment target
              ) ++
              commonNativeOptions
          )
      }
    )
    .dependsOn(nativeBridge)

lazy val linkLibraries = taskKey[Unit]("Link the libraries for iOS and simulator")

lazy val buildFramework = taskKey[Unit]("Build the framework for xcode")

linkLibraries := {
  (nativeBridgeSimulator / Compile / nativeLink).value
  (nativeBridgeIos / Compile / nativeLink).value
}

buildFramework := {
  Process("./framework.sh").!!
}

lazy val root =
  project
    .in(file("."))
    .settings(
      name := "xcode-native-sandbox",
    )
    .aggregate(
      nativeBridge,
      nativeBridgeIos,
      nativeBridgeSimulator,
    )
