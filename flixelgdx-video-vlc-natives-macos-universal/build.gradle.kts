plugins {
  id("flixelgdx.java-library")
}

val vlcVersionString = libs.versions.vlc.get()

val vlcDownloads = listOf(
  mapOf(
    "name" to "vlc-${vlcVersionString}-universal.dmg",
    "url" to "https://download.videolan.org/pub/videolan/vlc/${vlcVersionString}/macosx/vlc-${vlcVersionString}-universal.dmg",
    "sha256" to "56ee657c3aaf5c71b4ab7d6e4f4a77f6eca54633e0bf42a93b8116eb1d1f6ec9"
  )
)

val vlcPluginBlocklist = listOf(
  "gui", "lua", "control", "services_discovery", "visualization",
  "mux", "stream_out", "access_output", "meta_engine", "keystore", "logger"
)

val vlcDownloadDir = layout.buildDirectory.dir("vlc-downloads")
val vlcNativesDir = layout.buildDirectory.dir("vlc-natives")

if ((findProperty("packageVlcNatives") ?: "false") == "true") {
  val downloadVlcNatives = tasks.register<DownloadVlcNativesTask>("downloadVlcNatives") {
    group = "flixelgdx"
    description = "Downloads libvlc $vlcVersionString macOS natives for JAR packaging."
    vlcVersion.set(vlcVersionString)
    downloadSpecs.set(vlcDownloads)
    pluginBlocklist.set(vlcPluginBlocklist)
    downloadCacheDir.set(vlcDownloadDir)
    nativesDir.set(vlcNativesDir)
  }

  tasks.processResources {
    dependsOn(downloadVlcNatives)
    from(vlcNativesDir) {
      into("org/flixelgdx/video/natives")
    }
  }
}
