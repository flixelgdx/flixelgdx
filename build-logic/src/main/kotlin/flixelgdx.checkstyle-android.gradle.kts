plugins {
  checkstyle
}

checkstyle {
  toolVersion = "10.21.0"
  configDirectory.set(layout.projectDirectory.dir("gradle/checkstyle"))
}

// Registered on the root project so Android sources are linted even when the Android module is
// not included in the build (it needs no Android SDK). The Checkstyle plugin only creates
// per-source-set tasks for Java source sets, so checkstyleMain is registered by hand.
val checkstyleMain = tasks.register<Checkstyle>("checkstyleMain") {
  group = "verification"
  description = "Runs Checkstyle on the Android main sources."
  source(layout.projectDirectory.dir("flixelgdx-android/src/main/java"))
  include("**/*.java")
  classpath = files()
}

tasks.named("check") {
  dependsOn(checkstyleMain)
}
