plugins {
  id("flixelgdx.java-library")
}

// Shared Java wrapper and C source for the miniaudio audio engine. Desktop and Android both
// consume this module; it must stay pure Java 17 with no platform-specific APIs so D8 can
// compile it against Android API 24 without issues.

dependencies {
  api(project(":flixelgdx-core"))
  implementation(libs.jetbrains.annotations)
}
