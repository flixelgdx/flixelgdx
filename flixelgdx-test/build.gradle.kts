plugins {
  id("flixelgdx.java-base")
  java
  // Measures which lines of the framework the tests run. The aggregation plugin reports on every project this
  // module depends on (core, gpu and jvm), not only on this module's own (empty) main code.
  `jacoco-report-aggregation`
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(25)
  }
}

dependencies {
  implementation(project(":flixelgdx-core"))
  implementation(project(":flixelgdx-gpu"))
  // Shared JVM code, such as the WebSocket transport the remote debugger uses on desktop and Android.
  implementation(project(":flixelgdx-jvm"))
  implementation(libs.jetbrains.annotations)

  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)

  // The GPU tests run the real renderer on WebGPU's null backend, through the Java FFM bindings. The natives of
  // every desktop platform are listed so the tests run on any machine a contributor uses.
  testImplementation(libs.jwebgpu.ffm)
  testRuntimeOnly(libs.jwebgpu.natives.linux.x64)
  testRuntimeOnly(libs.jwebgpu.natives.linux.aarch64)
  testRuntimeOnly(libs.jwebgpu.natives.windows.x64)
  testRuntimeOnly(libs.jwebgpu.natives.windows.aarch64)
  testRuntimeOnly(libs.jwebgpu.natives.macos.x64)
  testRuntimeOnly(libs.jwebgpu.natives.macos.aarch64)

  // Runs the @JsonSerializable annotation processor over the test sources so the generated
  // serializers can be exercised by the round-trip test.
  testAnnotationProcessor(project(":flixelgdx-json-processor"))
}

jacoco {
  // 0.8.14 is the first release that officially reads Java 25 class files.
  toolVersion = libs.versions.jacoco.get()
}

// Code that only forwards to a platform backend is left out of the coverage numbers: the audio package (real playback
// lives in each backend) and the backend interfaces with their no-op defaults in org.flixelgdx.backend itself. Its
// sub-packages, such as backend.gpu and backend.jvm, hold real logic and stay included.
val coverageExcludes = listOf(
  "org/flixelgdx/audio/**",
  "org/flixelgdx/backend/*.class"
)

val coverageReport = tasks.named<JacocoReport>("testCodeCoverageReport") {
  val classes = classDirectories.from.toList()
  classDirectories.setFrom(files(classes).asFileTree.matching { exclude(coverageExcludes) })
}

// A run narrowed with --tests covers only part of the framework, so it skips the coverage check instead of failing it.
val narrowedTestRun = gradle.startParameter.taskRequests.any { "--tests" in it.args }

// Fails the build when the tests run less than 80% of the framework's lines or 70% of its branches.
val coverageVerification = tasks.register<JacocoCoverageVerification>("testCodeCoverageVerification") {
  group = "verification"
  description = "Fails when the unit tests cover less than 80% of lines or 70% of branches."
  executionData.from(coverageReport.map { it.executionData })
  classDirectories.from(coverageReport.map { it.classDirectories })
  sourceDirectories.from(coverageReport.map { it.sourceDirectories })
  onlyIf("the whole test suite ran") { !narrowedTestRun }
  violationRules {
    rule {
      limit {
        counter = "LINE"
        minimum = "0.80".toBigDecimal()
      }
      limit {
        counter = "BRANCH"
        minimum = "0.70".toBigDecimal()
      }
    }
  }
}

tasks.named<Test>("test") {
  useJUnitPlatform()

  // ./gradlew test always builds the coverage report and checks the minimum.
  finalizedBy(coverageReport, coverageVerification)

  // The GPU smoke test calls the WebGPU library through Java's foreign function API.
  jvmArgs("--enable-native-access=ALL-UNNAMED")
}
