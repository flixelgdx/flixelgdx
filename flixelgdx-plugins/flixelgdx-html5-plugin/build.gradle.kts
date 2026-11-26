plugins {
  id("flixelgdx.gradle-plugin")
}

gradlePlugin {
  plugins {
    create("html5") {
      id = "org.flixelgdx.html5"
      implementationClass = "org.flixelgdx.gradle.html5.Html5Plugin"
      displayName = "FlixelGDX HTML5 Plugin"
      description =
        "Automates web asset copying, index.html generation, and task wiring for FlixelGDX TeaVM web builds."
    }
  }
}

dependencies {
  compileOnly(libs.teavm.gradle.plugin)
}

dependencies {
  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(libs.teavm.gradle.plugin)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}
