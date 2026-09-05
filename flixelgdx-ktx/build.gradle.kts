plugins {
  id("flixelgdx.kotlin-library")
}

dependencies {
  api(project(":flixelgdx-core"))
  implementation(libs.jetbrains.annotations)
}
