plugins {
  id("com.diffplug.spotless")
}

spotless {
  java {
    target("flixelgdx-android/src/**/*.java")
    flixelRules(rootDir)
  }
}
