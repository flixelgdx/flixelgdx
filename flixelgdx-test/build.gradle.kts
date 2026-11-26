plugins {
  id("flixelgdx.java-base")
  java
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
  implementation(project(":flixelgdx-jvm"))
  implementation(libs.jetbrains.annotations)

  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)

  testImplementation(libs.jwebgpu.ffm)
  testRuntimeOnly(libs.jwebgpu.natives.linux.x64)
  testRuntimeOnly(libs.jwebgpu.natives.linux.aarch64)
  testRuntimeOnly(libs.jwebgpu.natives.windows.x64)
  testRuntimeOnly(libs.jwebgpu.natives.windows.aarch64)
  testRuntimeOnly(libs.jwebgpu.natives.macos.x64)
  testRuntimeOnly(libs.jwebgpu.natives.macos.aarch64)

  testAnnotationProcessor(project(":flixelgdx-json-processor"))
}

jacoco {
  toolVersion = libs.versions.jacoco.get()
}

val coverageExcludes = listOf(
  "org/flixelgdx/audio/**",
  "org/flixelgdx/backend/*.class"
)

val coverageReport = tasks.named<JacocoReport>("testCodeCoverageReport") {
  val classes = classDirectories.from.toList()
  classDirectories.setFrom(files(classes).asFileTree.matching { exclude(coverageExcludes) })
}

val narrowedTestRun = gradle.startParameter.taskRequests.any { "--tests" in it.args }

val coverageVerification = tasks.register<JacocoCoverageVerification>("testCodeCoverageVerification") {
  group = "verification"
  description = "Fails when the unit tests cover less than 70% of lines or 60% of branches."
  executionData.from(coverageReport.map { it.executionData })
  classDirectories.from(coverageReport.map { it.classDirectories })
  sourceDirectories.from(coverageReport.map { it.sourceDirectories })
  onlyIf("the whole test suite ran") { !narrowedTestRun }
  violationRules {
    rule {
      limit {
        counter = "LINE"
        minimum = "0.70".toBigDecimal()
      }
      limit {
        counter = "BRANCH"
        minimum = "0.60".toBigDecimal()
      }
    }
  }
}

tasks.named<Test>("test") {
  useJUnitPlatform()

  finalizedBy(coverageReport, coverageVerification)

  jvmArgs("--enable-native-access=ALL-UNNAMED")
}
