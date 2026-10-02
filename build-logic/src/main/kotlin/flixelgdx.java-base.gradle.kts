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

spotless {
  java {
    flixelRules(rootDir)
  }
}
