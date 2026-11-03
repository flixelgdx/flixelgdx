plugins {
  id("flixelgdx.java-library")
  alias(libs.plugins.teavm)
}

dependencies {
  api(project(":flixelgdx-core"))
  api(project(":flixelgdx-gpu"))
  api(libs.jwebgpu.teavm)

  implementation(libs.jetbrains.annotations)
  compileOnly(libs.teavm.core)
}
