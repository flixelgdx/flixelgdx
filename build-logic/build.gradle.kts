/**
 * Build logic for FlixelGDX convention plugins.
 *
 * <p>Every plugin declared in src/main/kotlin/ is compiled against the dependencies listed here,
 * so their types and extensions are available to the precompiled script plugins without needing
 * a buildscript block in each applying project.
 */
plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
  gradlePluginPortal()
  google()
  maven("https://s01.oss.sonatype.org")
  maven("https://oss.sonatype.org/content/repositories/snapshots/")
  maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
  implementation("com.diffplug.spotless:spotless-plugin-gradle:8.6.0")
  implementation("com.vanniktech:gradle-maven-publish-plugin:0.33.0")
  implementation("com.android.tools.build:gradle:8.7.3")
  // Needed by DownloadVlcNativesTask to extract .deb, .7z, and tar archives in-process.
  implementation("org.apache.commons:commons-compress:1.27.1")
  implementation("org.tukaani:xz:1.10")
}
