plugins {
  id("flixelgdx.java-library")
}

dependencies {
  api(project(":flixelgdx-core"))
  api(libs.jwebgpu.api)

  implementation(libs.jetbrains.annotations)
}
