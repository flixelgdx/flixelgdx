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

dependencies {
  implementation(libs.jetbrains.annotations)

  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
}

// The functional tests run real Gradle builds, so they reuse this build's Gradle user home for its caches.
tasks.named<Test>("test") {
  useJUnitPlatform()
  systemProperty("flixel.test.gradleUserHome", gradle.gradleUserHomeDir.absolutePath)
}
