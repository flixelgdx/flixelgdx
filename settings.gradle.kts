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

// jwebgpu (the WebGPU bindings every renderer is written against) is consumed as a composite build until it is
// published. The checkout is looked up, in order, from: -Pjwebgpu.dir=..., jwebgpu.dir in gradle.properties or
// local.properties, the JWEBGPU_DIR environment variable, and finally the sibling folder ../jwebgpu.
val jwebgpuDir = run {
  val fromCli = startParameter.projectProperties["jwebgpu.dir"]
  val fromGradle = providers.gradleProperty("jwebgpu.dir").orNull
  val fromLocal = File(settingsDir, "local.properties").takeIf { it.exists() }?.let { f ->
    Properties().also { p -> f.inputStream().use(p::load) }.getProperty("jwebgpu.dir")
  }
  val fromEnv = System.getenv("JWEBGPU_DIR")
  val raw = listOf(fromCli, fromGradle, fromLocal, fromEnv).firstOrNull { !it.isNullOrBlank() } ?: "../jwebgpu"
  File(raw).let { if (it.isAbsolute) it else File(settingsDir, raw).canonicalFile }
}
if (!File(jwebgpuDir, "settings.gradle.kts").exists()) {
  throw GradleException(
    """
    FlixelGDX needs the jwebgpu checkout, but nothing was found at: ${jwebgpuDir.absolutePath}

    jwebgpu is not published yet, so clone it next to the FlixelGDX folder:

        git clone https://github.com/stringdotjar/jwebgpu.git ${File(settingsDir.parentFile, "jwebgpu").absolutePath}

    To keep it somewhere else, pass -Pjwebgpu.dir=/path/to/jwebgpu, or add jwebgpu.dir=/path/to/jwebgpu to
    local.properties (or set the JWEBGPU_DIR environment variable).
    """.trimIndent()
  )
}
includeBuild(jwebgpuDir)

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
