plugins {
  id("flixelgdx.java-library")
}

// Embed version from build so Flixel.getVersion() can read it at runtime (Gradle does not set
// JAR manifest like Maven does).
val generateFlixelVersion = tasks.register<WriteProperties>("generateFlixelVersion") {
  destinationFile = layout.buildDirectory.file("generated/version/version.properties")
  property("version", project.version)
}
tasks.processResources {
  from(generateFlixelVersion) {
    into("org/flixelgdx")
  }
}

dependencies {
  // The core module is backend-agnostic: it depends on no GPU or platform library. All rendering,
  // input, audio, and file access flow through the seams the platform backends implement.
  implementation(libs.jetbrains.annotations)
}
