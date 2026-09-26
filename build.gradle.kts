/**
 * Root aggregator for the FlixelGDX multi-module build.
 *
 * The root project holds no source itself; it only applies IDE plugins and registers the
 * aggregate Javadoc task. All subproject setup lives in the convention plugins under
 * build-logic/src/main/kotlin/.
 */

plugins {
  eclipse
  idea
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.vanniktech) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.jvm) apply false
}

val groupId: String by project

group = groupId
version = gitVersion()

fun gitVersion(): String = try {
  val proc = ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
    .directory(rootDir)
    .start()
  proc.waitFor()
  proc.inputStream.bufferedReader().readText().trim().removePrefix("v").ifEmpty { "unspecified" }
} catch (_: Exception) {
  "unspecified"
}

eclipse.project.name = "flixelgdx-parent"

idea {
  module {
    outputDir = file("build/classes/java/main")
    testOutputDir = file("build/classes/java/test")
  }
}

tasks.register("javadocAll") {
  group = "verification"
  description = "Runs Javadoc (with doclint) on all published Java library modules."
  val modules = arrayListOf(
    ":flixelgdx-core:javadoc",
    ":flixelgdx-miniaudio:javadoc",
    ":flixelgdx-desktop:javadoc",
    ":flixelgdx-html5:javadoc",
    ":flixelgdx-ios:javadoc",
    ":flixelgdx-json-processor:javadoc",
    ":flixelgdx-plugins:flixelgdx-basisu-plugin:javadoc",
    ":flixelgdx-plugins:flixelgdx-html5-plugin:javadoc",
    ":flixelgdx-plugins:flixelgdx-logging-plugin:javadoc",
    ":flixelgdx-plugins:flixelgdx-shader-plugin:javadoc"
  )
  if (gradle.extra["includeAndroid"] as Boolean) {
    modules.add(":flixelgdx-android:javadoc")
  }
  dependsOn(modules)
}
