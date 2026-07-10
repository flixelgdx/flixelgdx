plugins {
  id("flixelgdx.java-library")
}

dependencies {
  api(project(":flixelgdx-core"))
  api(libs.miniaudio)
  implementation(libs.jetbrains.annotations)
}
