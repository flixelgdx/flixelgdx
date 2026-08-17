plugins {
  id("flixelgdx.kotlin-library")
}

dependencies {
  // The KTX module only adds idiomatic Kotlin syntax on top of the backend-agnostic core surface;
  // it pulls in nothing platform-specific. It is exposed with `api` so Kotlin consumers that depend
  // on this module also see the core types the extensions operate on.
  api(project(":flixelgdx-core"))
  implementation(libs.jetbrains.annotations)
}
