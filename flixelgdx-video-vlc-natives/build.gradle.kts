// Packages libvlc native libraries for Windows, macOS, and Linux into a single JAR.
// flixelgdx-video-lwjgl3 pulls this as a runtimeOnly dependency; FlixelVlcDiscovery
// extracts the correct platform's files to a per-user cache at runtime.
//
// Download and packaging are gated behind -PpackageVlcNatives=true so normal developer
// builds stay fast and network-free. The publish workflow sets this flag when cutting a
// release so that the natives JAR is always up to date on Maven Central.

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
val vlcNativesDir = layout.buildDirectory.dir("vlc-natives")

if ((findProperty("packageVlcNatives") ?: "false") == "true") {
  val downloadVlcNatives = tasks.register<DownloadVlcNativesTask>("downloadVlcNatives") {
    group = "flixelgdx"
    description = "Downloads libvlc $vlcVersionString natives (Windows, macOS, Linux) for JAR packaging."
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
      exclude("**/sdk/**")
    }
  }
}
