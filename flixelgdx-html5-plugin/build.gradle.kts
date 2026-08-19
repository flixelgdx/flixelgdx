plugins {
  id("flixelgdx.gradle-plugin")
}

gradlePlugin {
  plugins {
    create("flixelHtml5") {
      id = "org.flixelgdx.html5"
      implementationClass = "org.flixelgdx.gradle.teavm.FlixelHtml5Plugin"
      displayName = "FlixelGDX HTML5 Plugin"
      description =
        "Automates web asset copying, index.html generation, and task wiring for FlixelGDX TeaVM web builds."
    }
  }
}

dependencies {
  // The TeaVM Gradle plugin types (TeaVMExtension, TeaVMJSConfiguration, etc.) are provided by
  // the user's build when they apply 'org.teavm' before this plugin, so we only need them at
  // compile time.
  compileOnly(libs.teavm.gradle.plugin)
}
