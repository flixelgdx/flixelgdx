plugins {
  id("flixelgdx.java-library")
}
val lwjglVersion = libs.versions.lwjgl.get()

val lwjglNatives = listOf(
  "natives-linux",
  "natives-macos",
  "natives-macos-arm64",
  "natives-windows"
)

dependencies {
  api(project(":flixelgdx-core"))
  api(project(":flixelgdx-gpu"))
  api(project(":flixelgdx-miniaudio"))
  api(project(":flixelgdx-jvm"))

  api(libs.jwebgpu.ffm)
  runtimeOnly(libs.jwebgpu.natives.linux.x64)
  runtimeOnly(libs.jwebgpu.natives.linux.aarch64)
  runtimeOnly(libs.jwebgpu.natives.windows.x64)
  runtimeOnly(libs.jwebgpu.natives.windows.aarch64)
  runtimeOnly(libs.jwebgpu.natives.macos.x64)
  runtimeOnly(libs.jwebgpu.natives.macos.aarch64)

  api(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
  api(libs.lwjgl)
  api(libs.lwjgl.sdl)
  api(libs.lwjgl.stb)

  lwjglNatives.forEach { classifier ->
    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$classifier")
    runtimeOnly("org.lwjgl:lwjgl-sdl:$lwjglVersion:$classifier")
    runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:$classifier")
  }

  implementation(libs.jetbrains.annotations)
  implementation(libs.jansi)

  compileOnly(libs.graalvm.nativeimage)
}
