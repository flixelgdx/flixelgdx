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
  api(project(":flixelgdx-miniaudio"))

  api(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
  api(libs.lwjgl)
  api(libs.lwjgl.sdl)
  api(libs.lwjgl.bgfx)
  api(libs.lwjgl.stb)

  lwjglNatives.forEach { classifier ->
    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$classifier")
    runtimeOnly("org.lwjgl:lwjgl-sdl:$lwjglVersion:$classifier")
    runtimeOnly("org.lwjgl:lwjgl-bgfx:$lwjglVersion:$classifier")
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
