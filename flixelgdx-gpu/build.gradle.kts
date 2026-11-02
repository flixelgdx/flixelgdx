plugins {
  id("flixelgdx.java-library")
}

dependencies {
  api(project(":flixelgdx-core"))

  // The platform-neutral WebGPU API. The backend that implements it (FFM on desktop, JNI on Android, TeaVM on
  // the web) is supplied by each platform module, never by this one.
  api(libs.jwebgpu.api)

  implementation(libs.jetbrains.annotations)
}
