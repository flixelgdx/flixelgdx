import java.util.Properties

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
  "flixelgdx-jvm",
  "flixelgdx-desktop",
  "flixelgdx-html5",
  "flixelgdx-ios",
  "flixelgdx-json-processor",
  ":flixelgdx-plugins:flixelgdx-assets-plugin",
  ":flixelgdx-plugins:flixelgdx-html5-plugin",
  ":flixelgdx-plugins:flixelgdx-project-plugin",
  ":flixelgdx-plugins:flixelgdx-basisu-plugin",
  ":flixelgdx-plugins:flixelgdx-shader-plugin",
  ":flixelgdx-plugins:flixelgdx-packagr-plugin",
  "flixelgdx-test"
)

if (includeAndroid) {
  include("flixelgdx-android")
}
