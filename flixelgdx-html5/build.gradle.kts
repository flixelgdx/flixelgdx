plugins {
  id("flixelgdx.java-library")
  alias(libs.plugins.teavm)
}

dependencies {
  api(project(":flixelgdx-core"))
  implementation(libs.jetbrains.annotations)

  // Only the TeaVM compiler loads the log call site plugin, and it already has teavm-core on its own
  // classpath. Nothing at runtime touches these classes, so they must not be shipped to games.
  compileOnly(libs.teavm.core)
}
