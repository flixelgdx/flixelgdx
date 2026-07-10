plugins {
  id("flixelgdx.java-base")
  java
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(17)
  }
}

dependencies {
  implementation(project(":flixelgdx-core"))

  testRuntimeOnly(libs.gdx.controllers.desktop)
  testRuntimeOnly(libs.junit.platform.launcher)
  testRuntimeOnly("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-desktop")
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.gdx.backend.headless)
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}
