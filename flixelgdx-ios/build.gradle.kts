plugins {
  id("flixelgdx.java-library")
}

// The iOS backend is a fail-fast placeholder until it is brought onto the framework's bgfx + SDL3
// stack in a later phase. It depends only on the core API so the module compiles with no libGDX or
// RoboVM on the classpath.
dependencies {
  api(project(":flixelgdx-core"))

  implementation(libs.jetbrains.annotations)
}
