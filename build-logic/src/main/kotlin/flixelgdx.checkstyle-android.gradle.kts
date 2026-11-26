plugins {
  checkstyle
}

checkstyle {
  toolVersion = "10.21.0"
  configDirectory.set(layout.projectDirectory.dir("gradle/checkstyle"))
}

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
