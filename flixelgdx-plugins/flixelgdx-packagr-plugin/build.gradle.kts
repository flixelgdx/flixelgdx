plugins {
  id("flixelgdx.gradle-plugin")
}

gradlePlugin {
  plugins {
    create("packagr") {
      id = "org.flixelgdx.packagr"
      implementationClass = "org.flixelgdx.gradle.packagr.PackagrPlugin"
      displayName = "FlixelGDX packagr Plugin"
      description =
        "Packages a FlixelGDX game into a self-contained native app per platform, bundling a trimmed JDK runtime."
    }
  }
}

dependencies {
  implementation(libs.commons.compress)
}
