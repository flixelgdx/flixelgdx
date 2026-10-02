/**
 * Baseline convention applied to every FlixelGDX Java subproject.
 *
 * <p>Covers: project coordinates, IDE metadata (Eclipse + IntelliJ), centralized repository
 * declarations, Java compile encoding, and Spotless formatting rules. Modules that need
 * publication or the {@code java-library} surface area apply {@code flixelgdx.java-library}
 * instead, which in turn applies this plugin.
 */

plugins {
  eclipse
  idea
  id("com.diffplug.spotless")
}

val groupId: String by project

group = groupId
version = rootProject.version

eclipse.project.name = project.name

idea {
  module {
    outputDir = file("build/classes/java/main")
    testOutputDir = file("build/classes/java/test")
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
}

// Android modules have no Java source sets for Spotless to discover, so their sources are
// formatted by the root project instead (see flixelgdx.spotless-android).
spotless {
  java {
    flixelRules(rootDir)
  }
}
