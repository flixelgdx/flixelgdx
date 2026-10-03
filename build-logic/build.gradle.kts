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
  implementation("com.diffplug.spotless:spotless-plugin-gradle:8.10.3")
  implementation("com.vanniktech:gradle-maven-publish-plugin:0.33.0")
  implementation("com.android.tools.build:gradle:8.7.3")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.20")
}

kotlin {
  jvmToolchain(17)
}
