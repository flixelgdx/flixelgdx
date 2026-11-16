plugins {
  id("flixelgdx.gradle-plugin")
}

gradlePlugin {
  plugins {
    create("assets") {
      id = "org.flixelgdx.assets"
      implementationClass = "org.flixelgdx.gradle.assets.AssetsPlugin"
      displayName = "FlixelGDX Assets Plugin"
      description =
        "Generates a type-safe Assets class with a constant for every file in a game's assets folder."
    }
  }
}
