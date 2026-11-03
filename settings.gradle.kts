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

// jwebgpu (the WebGPU bindings every renderer is written against) comes from Maven Central, at the version in
// gradle/libs.versions.toml. To build against a local jwebgpu checkout instead (for example to test a change to it),
// point at it with -Pjwebgpu.dir=..., jwebgpu.dir in gradle.properties or local.properties, or the JWEBGPU_DIR
// environment variable. Gradle then substitutes the local projects for the published ones.
val jwebgpuDir = run {
  val fromCli = startParameter.projectProperties["jwebgpu.dir"]
  val fromGradle = providers.gradleProperty("jwebgpu.dir").orNull
  val fromLocal = File(settingsDir, "local.properties").takeIf { it.exists() }?.let { f ->
    Properties().also { p -> f.inputStream().use(p::load) }.getProperty("jwebgpu.dir")
  }
  val fromEnv = System.getenv("JWEBGPU_DIR")
  listOf(fromCli, fromGradle, fromLocal, fromEnv).firstOrNull { !it.isNullOrBlank() }
    ?.let { raw -> File(raw).let { if (it.isAbsolute) it else File(settingsDir, raw).canonicalFile } }
}
if (jwebgpuDir != null) {
  if (!File(jwebgpuDir, "settings.gradle.kts").exists()) {
    throw GradleException("jwebgpu.dir points at ${jwebgpuDir.absolutePath}, which is not a jwebgpu checkout.")
  }
  includeBuild(jwebgpuDir)
}

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
  "flixelgdx-gpu",
  "flixelgdx-miniaudio",
  "flixelgdx-desktop",
  "flixelgdx-html5",
  "flixelgdx-ios",
  "flixelgdx-json-processor",
  ":flixelgdx-plugins:flixelgdx-html5-plugin",
  ":flixelgdx-plugins:flixelgdx-basisu-plugin",
  ":flixelgdx-plugins:flixelgdx-shader-plugin",
  ":flixelgdx-plugins:flixelgdx-packagr-plugin",
  "flixelgdx-test"
)

if (includeAndroid) {
  include("flixelgdx-android")
}
