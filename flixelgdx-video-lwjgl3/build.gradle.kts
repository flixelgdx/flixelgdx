// Desktop backend for the FlixelGDX video extension. Bridges libvlc into the framework through
// JNA-registered JNI bindings. The libvlc native libraries are provided at runtime by
// flixelgdx-video-vlc-natives, which FlixelVlcDiscovery extracts from the classpath on first use.

plugins {
  id("flixelgdx.java-library")
}

dependencies {
  api(project(":flixelgdx-video-core"))
  api(libs.jna)
  implementation(project(":flixelgdx-lwjgl3"))
  implementation(libs.gdx.backend.lwjgl3)
  implementation(libs.jetbrains.annotations)
  runtimeOnly(project(":flixelgdx-video-vlc-natives"))

  // Provides the Feature interface and RuntimeJNIAccess used by FlixelVideoGraalFeature.
  // Compile-only: the class runs inside native-image at build time, not in user runtimes.
  compileOnly(libs.graalvm.nativeimage)
}
