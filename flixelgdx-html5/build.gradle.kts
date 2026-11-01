plugins {
  id("flixelgdx.java-library")
  alias(libs.plugins.teavm)
}

dependencies {
  api(project(":flixelgdx-core"))
  api(project(":flixelgdx-gpu"))

  // The WebGPU backend for the browser. It wraps navigator.gpu, so it carries no native code.
  api(libs.jwebgpu.teavm)

  implementation(libs.jetbrains.annotations)
  compileOnly(libs.teavm.core)
}
