plugins {
  id("flixelgdx.gradle-plugin")
}

gradlePlugin {
  plugins {
    create("logging") {
      id = "org.flixelgdx.logging"
      implementationClass = "org.flixelgdx.gradle.logging.LoggingPlugin"
      displayName = "FlixelGDX Logging Plugin"
      description = "Weaves explicit source file and line into FlixelLogger calls after compilation for accurate traces on all platforms."
    }
  }
}

dependencies {
  implementation(libs.asm)
  implementation(libs.asm.tree)
}
