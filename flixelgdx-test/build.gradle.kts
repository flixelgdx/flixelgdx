plugins {
  id("flixelgdx.java-base")
  java
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

tasks.named<Test>("test") {
  useJUnitPlatform()

  // The GPU smoke test calls the WebGPU library through Java's foreign function API.
  jvmArgs("--enable-native-access=ALL-UNNAMED")
}
