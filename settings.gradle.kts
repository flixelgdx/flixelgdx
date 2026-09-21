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
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
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
    mavenLocal()
    maven("https://s01.oss.sonatype.org")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
  }
}

rootProject.name = "flixelgdx"

include(
  "flixelgdx-core",
  "flixelgdx-desktop",
  "flixelgdx-html5",
  "flixelgdx-ios",
  "flixelgdx-jvm",
  "flixelgdx-json-processor",
  ":flixelgdx-plugins:flixelgdx-html5-plugin",
  ":flixelgdx-plugins:flixelgdx-logging-plugin",
  ":flixelgdx-plugins:flixelgdx-basisu-plugin",
  ":flixelgdx-plugins:flixelgdx-shader-plugin",
  "flixelgdx-test"
)

if (includeAndroid) {
  include("flixelgdx-android")
}
