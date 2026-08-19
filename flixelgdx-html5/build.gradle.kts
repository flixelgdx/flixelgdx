plugins {
  id("flixelgdx.java-library")
  alias(libs.plugins.teavm)
}

dependencies {
  api(project(":flixelgdx-core"))
  implementation(libs.jetbrains.annotations)
}
