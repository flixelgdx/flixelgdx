plugins {
  id("flixelgdx.java-library")
}

// Code that every JVM backend (desktop and Android) shares. It must stay inside the Java APIs that Android has, and
// it must not be a dependency of the web backend, which has no sockets.
dependencies {
  api(project(":flixelgdx-core"))
  implementation(libs.jetbrains.annotations)
}
