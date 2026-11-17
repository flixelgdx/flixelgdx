plugins {
  id("flixelgdx.gradle-plugin")
}

gradlePlugin {
  plugins {
    create("project") {
      id = "org.flixelgdx.project"
      implementationClass = "org.flixelgdx.gradle.project.ProjectPlugin"
      displayName = "FlixelGDX Project Plugin"
      description =
        "Reads a game's project.toml to pick its modules, name and version, and to generate its FlixelConfig values."
    }
  }
}

dependencies {
  implementation(libs.jetbrains.annotations)
  implementation(libs.tomlj)

  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
}

// The functional tests run real Gradle builds, so they reuse this build's Gradle user home for its caches.
tasks.named<Test>("test") {
  useJUnitPlatform()
  systemProperty("flixel.test.gradleUserHome", gradle.gradleUserHomeDir.absolutePath)
}
