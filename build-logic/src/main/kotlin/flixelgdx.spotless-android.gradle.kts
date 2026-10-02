/**
 * Spotless coverage for the Android module's Java sources, applied to the root project.
 *
 * <p>The {@code flixelgdx-android} module is only part of the build when it is included with
 * {@code -PincludeAndroid=true} (it needs the Android SDK), and the Android Gradle plugin does
 * not expose Java source sets that Spotless can discover. Formatting those sources from the
 * root project with an explicit target means a plain {@code ./gradlew spotlessApply} and
 * {@code ./gradlew spotlessCheck} always cover them, with or without the Android SDK. The rules
 * come from {@code flixelRules()}, the same function every other module uses.
 */

plugins {
  id("com.diffplug.spotless")
}

spotless {
  java {
    target("flixelgdx-android/src/**/*.java")
    flixelRules(rootDir)
  }
}
