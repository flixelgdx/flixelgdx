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
  api(libs.gdx)
  api(libs.gdx.freetype)
  api(libs.gdx.controllers.core)
  api(libs.basisu.gdx)
  api(libs.anim8)
  api(libs.utils)

  implementation(libs.jetbrains.annotations)
}
