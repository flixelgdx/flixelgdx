plugins {
  id("flixelgdx.gradle-plugin")
}

gradlePlugin {
  plugins {
    create("flixelShaders") {
      id = "org.flixelgdx.shaders"
      implementationClass = "org.flixelgdx.gradle.shader.FlixelShaderPlugin"
      displayName = "FlixelGDX Shader Plugin"
      description =
        "Cross-compiles a single GLSL shader pair into every FlixelGDX backend variant at build time."
    }
  }
}

dependencies {
  implementation(libs.jetbrains.annotations)
}
