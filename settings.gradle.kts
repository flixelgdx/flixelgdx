import java.util.Properties

/**
 * Root settings for the FlixelGDX multi-module build.
 *
 * <p>Declares the build-logic included build so convention plugins are available to all
 * subprojects, centralizes repository declarations, and conditionally includes the Android
 * modules when the Android SDK is present.
 */

pluginManagement {
  includeBuild("build-logic")
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    maven("https://s01.oss.sonatype.org")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

// Android modules are optional so the framework can be built without an Android SDK.
// Enable via: -PincludeAndroid=true (CI / one-off) or includeAndroid=true in local.properties (gitignored).
val includeAndroidFromCli = startParameter.projectProperties["includeAndroid"] == "true"
val includeAndroidFromLocal = run {
  val f = File(settingsDir, "local.properties")
  if (f.exists()) {
    val props = Properties()
    f.inputStream().use(props::load)
    props.getProperty("includeAndroid", "false") == "true"
  } else {
    false
  }
}
val includeAndroid = includeAndroidFromCli || includeAndroidFromLocal
gradle.extra["includeAndroid"] = includeAndroid

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
  repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
    maven("https://s01.oss.sonatype.org")
    mavenLocal()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
  }
}

rootProject.name = "flixelgdx"

include(
  "flixelgdx-core",
  "flixelgdx-common",
  "flixelgdx-jvm",
  "flixelgdx-lwjgl3",
  "flixelgdx-ios",
  "flixelgdx-teavm",
  "flixelgdx-teavm-plugin",
  "flixelgdx-logging-plugin",
  "flixelgdx-basisu-plugin",
  "flixelgdx-video-core",
  "flixelgdx-video-lwjgl3",
  "flixelgdx-video-teavm",
  "flixelgdx-test"
)

if (includeAndroid) {
  include("flixelgdx-android", "flixelgdx-video-android")
}
