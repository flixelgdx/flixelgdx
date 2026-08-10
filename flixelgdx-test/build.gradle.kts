plugins {
  id("flixelgdx.java-base")
  java
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(17)
  }
}

dependencies {
  implementation(project(":flixelgdx-core"))
  // The JVM module supplies the real java.io file backend so save round-trip tests persist to disk.
  implementation(project(":flixelgdx-jvm"))
  implementation(libs.jetbrains.annotations)

  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)

  // Runs the @JsonSerializable annotation processor over the test sources so the generated
  // serializers can be exercised by the round-trip test.
  testAnnotationProcessor(project(":flixelgdx-json-processor"))
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}
