plugins {
  id("flixelgdx.java-library")
}

dependencies {
  api(project(":flixelgdx-video-core"))
  api(project(":flixelgdx-teavm"))
  implementation(libs.jetbrains.annotations)
}
