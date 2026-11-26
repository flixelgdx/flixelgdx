plugins {
  id("flixelgdx.java-library")
}

dependencies {
  implementation(libs.jetbrains.annotations)
}

val versionDir = layout.buildDirectory.dir("generated/version")
val generateVersion = tasks.register("generateVersion") {
  val version = provider { project.version.toString() }
  val dir = versionDir
  inputs.property("version", version)
  outputs.dir(dir)
  doLast {
    val file = dir.get().file("org/flixelgdx/FlixelVersion.java").asFile
    file.parentFile.mkdirs()
    file.writeText(
      """
      package org.flixelgdx;

      /**
       * The version of the FlixelGDX framework, generated at build time from the git tag.
       */
      public final class FlixelVersion {

        /** The framework version, for example {@code 1.2.3}, or {@code unspecified} for an untagged build. */
        public static final String VERSION = "${version.get()}";

        private FlixelVersion() {}
      }
      """.trimIndent() + "\n"
    )
  }
}

sourceSets.named("main") {
  java.srcDir(generateVersion.map { versionDir })
}

spotless {
  java {
    targetExclude("build/generated/**")
  }
}

tasks.named<Checkstyle>("checkstyleMain") {
  exclude("**/FlixelVersion.java")
}
