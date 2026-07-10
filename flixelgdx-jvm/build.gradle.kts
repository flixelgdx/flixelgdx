plugins {
  id("flixelgdx.java-library")
}

dependencies {
  api(project(":flixelgdx-common"))
  implementation(libs.jetbrains.annotations)
}
