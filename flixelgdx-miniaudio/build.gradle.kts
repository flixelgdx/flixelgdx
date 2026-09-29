plugins {
  id("flixelgdx.java-library")
}

dependencies {
  api(project(":flixelgdx-core"))
  implementation(libs.jetbrains.annotations)
}
