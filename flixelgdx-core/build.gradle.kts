plugins {
  id("flixelgdx.java-library")
}

dependencies {
  // The core module is backend-agnostic: it depends on no GPU or platform library. All rendering,
  // input, audio, and file access flow through the seams the platform backends implement.
  implementation(libs.jetbrains.annotations)
}
