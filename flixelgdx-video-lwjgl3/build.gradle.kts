// Desktop backend for the FlixelGDX video extension. Bridges libvlc into the framework through
// JNA-registered JNI bindings and packages (optionally) the libvlc native libraries that
// downloadVlcNatives fetches from upstream.

import java.util.Locale

plugins {
  id("flixelgdx.java-library")
}

val vlcVersionString = libs.versions.vlc.get()

val vlcDownloads = listOf(
  mapOf(
    "name" to "vlc-${vlcVersionString}-win64.zip",
    "url" to "https://download.videolan.org/pub/videolan/vlc/${vlcVersionString}/win64/vlc-${vlcVersionString}-win64.zip",
    "sha256" to "992d19dbd0b8a7cde9167d2f7780b1ef6f92acc8a71acfa736101a21f35181e1"
  ),
  mapOf(
    "name" to "vlc-${vlcVersionString}-win64.7z",
    "url" to "https://download.videolan.org/pub/videolan/vlc/${vlcVersionString}/win64/vlc-${vlcVersionString}-win64.7z",
    "sha256" to "eb4fd8a28291da73608c733786a09610fea865fbe94113bcb60b91c1ebb8404a"
  ),
  mapOf(
    "name" to "vlc-${vlcVersionString}-universal.dmg",
    "url" to "https://download.videolan.org/pub/videolan/vlc/${vlcVersionString}/macosx/vlc-${vlcVersionString}-universal.dmg",
    "sha256" to "56ee657c3aaf5c71b4ab7d6e4f4a77f6eca54633e0bf42a93b8116eb1d1f6ec9"
  ),
  mapOf(
    "name" to "libvlc5_${vlcVersionString}-0+deb12u1_amd64.deb",
    "url" to "https://deb.debian.org/debian/pool/main/v/vlc/libvlc5_${vlcVersionString}-0%2Bdeb12u1_amd64.deb",
    "sha256" to "07ad5c61dc41acf29c485224accf457b7632e68a910eee21badf30213e6ab359"
  ),
  mapOf(
    "name" to "libvlccore9_${vlcVersionString}-0+deb12u1_amd64.deb",
    "url" to "https://deb.debian.org/debian/pool/main/v/vlc/libvlccore9_${vlcVersionString}-0%2Bdeb12u1_amd64.deb",
    "sha256" to "a3114b86450777e4cbbd4620419b74d189367e5f5026286cc74c39c9e759bfa7"
  ),
  mapOf(
    "name" to "vlc-plugin-base_${vlcVersionString}-0+deb12u1_amd64.deb",
    "url" to "https://deb.debian.org/debian/pool/main/v/vlc/vlc-plugin-base_${vlcVersionString}-0%2Bdeb12u1_amd64.deb",
    "sha256" to "38b953a2a6355c5ba75e3e5d2015100d793fbf5a00211aaa78b2d705a9547cb1"
  ),
  mapOf(
    "name" to "vlc-plugin-video-output_${vlcVersionString}-0+deb12u1_amd64.deb",
    "url" to "https://deb.debian.org/debian/pool/main/v/vlc/vlc-plugin-video-output_${vlcVersionString}-0%2Bdeb12u1_amd64.deb",
    "sha256" to "de1d62487161efc62305b6e84cd364e71f109125d601401ee824d3291813e6e2"
  )
)

// Plugin categories no game playback path needs (interface, scripting, streaming out,
// discovery). Dropping them roughly halves the Windows natives payload.
val vlcPluginBlocklist = listOf(
  "gui", "lua", "control", "services_discovery", "visualization",
  "mux", "stream_out", "access_output", "meta_engine", "keystore", "logger"
)

val vlcDownloadDir = layout.buildDirectory.dir("vlc-downloads")
val vlcNativesDir = layout.projectDirectory.dir("natives")

val downloadVlcNatives = tasks.register<DownloadVlcNativesTask>("downloadVlcNatives") {
  group = "flixelgdx"
  description = "Downloads libvlc $vlcVersionString natives (Windows, macOS, Linux) into natives/."
  vlcVersion.set(vlcVersionString)
  downloadSpecs.set(vlcDownloads)
  pluginBlocklist.set(vlcPluginBlocklist)
  downloadCacheDir.set(vlcDownloadDir)
  nativesDir.set(vlcNativesDir)
}

val hostVlcPlatform = run {
  val os = System.getProperty("os.name", "").lowercase(Locale.ROOT)
  when {
    os.contains("win") -> "windows-amd64"
    os.contains("mac") -> "macos-universal"
    else -> "linux-amd64"
  }
}

fun vlcNativesComplete(platformDir: File): Boolean {
  if (!platformDir.isDirectory) return false
  fun hasLibIn(dir: File) = dir.isDirectory && (dir.listFiles()?.any { it.name.startsWith("libvlc") } ?: false)
  val hasLib = hasLibIn(platformDir) || hasLibIn(File(platformDir, "lib"))
  val pluginsDir = if (File(platformDir, "plugins").isDirectory) File(platformDir, "plugins")
  else File(platformDir, "vlc/plugins")
  val hasPlugins = pluginsDir.isDirectory && (pluginsDir.listFiles()?.isNotEmpty() ?: false)
  return hasLib && hasPlugins
}

if ((findProperty("skipVlcNatives") ?: "false") != "true") {
  tasks.processResources {
    dependsOn(downloadVlcNatives)
    // Fail loudly if the host platform's natives did not make it into natives/. Without this
    // the jar is packaged silently and the missing libraries only surface at runtime as an
    // unavailable video, which is exactly the failure this module tries to prevent.
    doFirst {
      val hostDir = File(vlcNativesDir.asFile, hostVlcPlatform)
      if (!vlcNativesComplete(hostDir)) {
        throw GradleException(
          "flixelgdx-video-lwjgl3: libvlc natives for this platform ($hostVlcPlatform) are " +
            "missing or incomplete under $hostDir. Run './gradlew " +
            ":flixelgdx-video-lwjgl3:downloadVlcNatives' (needs network access), or pass " +
            "-PskipVlcNatives=true to build a slim jar that relies on a system VLC install."
        )
      }
    }
    from(vlcNativesDir) {
      into("org/flixelgdx/video/natives")
      exclude("**/sdk/**")
    }
  }
}

dependencies {
  api(project(":flixelgdx-video-core"))
  api(libs.jna)
  implementation(project(":flixelgdx-lwjgl3"))
  implementation(libs.gdx.backend.lwjgl3)
  implementation(libs.jetbrains.annotations)

  // Provides the Feature interface and RuntimeJNIAccess used by FlixelVideoGraalFeature.
  // Compile-only: the class runs inside native-image at build time, not in user runtimes.
  compileOnly(libs.graalvm.nativeimage)
}
