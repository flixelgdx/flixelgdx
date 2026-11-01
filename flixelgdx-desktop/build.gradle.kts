plugins {
  id("flixelgdx.java-library")
}
val lwjglVersion = libs.versions.lwjgl.get()

// Native classifiers bundled with the backend so packaged games run out of the box.
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

  // The WebGPU backend for the desktop (Java FFM) plus the Dawn library of every desktop platform, so a packaged
  // game runs anywhere. Each natives jar is a few MB; a game that ships to fewer platforms can exclude the rest.
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

  api(libs.imgui.binding)
  runtimeOnly(libs.imgui.natives.linux)
  runtimeOnly(libs.imgui.natives.macos)
  runtimeOnly(libs.imgui.natives.windows)

  implementation(libs.jetbrains.annotations)
  implementation(libs.jansi)

  compileOnly(libs.graalvm.nativeimage)
}
