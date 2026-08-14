plugins {
  id("flixelgdx.java-library")
}

// The iOS backend is a fail-fast placeholder until it is brought onto the framework's bgfx + SDL3
// stack in a later phase.
dependencies {
  api(project(":flixelgdx-core"))

  implementation(libs.jetbrains.annotations)
}
