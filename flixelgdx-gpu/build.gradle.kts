plugins {
  id("flixelgdx.java-library")
}

dependencies {
  api(project(":flixelgdx-core"))

  // The platform-neutral WebGPU API. The backend that implements it (FFM on desktop, JNI on Android, TeaVM on
  // the web) is supplied by each platform module, never by this one.
  api(libs.jwebgpu.api)

  implementation(libs.jetbrains.annotations)
}

// The built-in sprite shader is compiled into the program as a string constant, so no platform has to read it
// from the classpath at run time (a browser has no classpath). src/main/wgsl/sprite.wgsl stays the single source.
val wgslSources = tasks.register<FlixelWgslSources>("generateWgslSources") {
  packageName = "org.flixelgdx.backend.gpu"
  className = "FlixelGpuShaders"
  shaders.put("SPRITE", layout.projectDirectory.file("src/main/wgsl/sprite.wgsl").asFile.path)
  outputDir = layout.buildDirectory.dir("generated/sources/wgsl/main/java")
}

sourceSets.main {
  java.srcDir(wgslSources)
}
