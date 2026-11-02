plugins {
  id("flixelgdx.java-library")
}

dependencies {
  implementation(libs.jetbrains.annotations)
}

// The WGSL preamble is compiled into the program as a string constant, so no platform has to read it from the
// classpath at run time (a browser has no classpath). src/main/wgsl/flixel_preamble.wgsl stays the single source.
val wgslSources = tasks.register<FlixelWgslSources>("generateWgslSources") {
  packageName = "org.flixelgdx.graphics.wgsl"
  className = "FlixelBuiltinWgsl"
  shaders.put("PREAMBLE", layout.projectDirectory.file("src/main/wgsl/flixel_preamble.wgsl").asFile.path)
  outputDir = layout.buildDirectory.dir("generated/sources/wgsl/main/java")
}

sourceSets.main {
  java.srcDir(wgslSources)
}
