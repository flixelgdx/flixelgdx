plugins {
  id("flixelgdx.java-library")
}

// The desktop backend binds SDL3 (windowing, input, gamepads) and bgfx (rendering) through
// LWJGL, plus stb (image decoding, TrueType/OpenType rasterization) and zstd (KTX2
// supercompression). The miniaudio audio engine ships as our own JNI natives under
// src/main/resources/org/flixelgdx/natives, so no extra dependency is needed for audio.
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
  api(project(":flixelgdx-jvm"))

  api(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
  api(libs.lwjgl)
  api(libs.lwjgl.sdl)
  api(libs.lwjgl.bgfx)
  api(libs.lwjgl.stb)
  api(libs.lwjgl.zstd)

  lwjglNatives.forEach { classifier ->
    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$classifier")
    runtimeOnly("org.lwjgl:lwjgl-sdl:$lwjglVersion:$classifier")
    runtimeOnly("org.lwjgl:lwjgl-bgfx:$lwjglVersion:$classifier")
    runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:$classifier")
    runtimeOnly("org.lwjgl:lwjgl-zstd:$lwjglVersion:$classifier")
  }

  // Dear ImGui (via the SpaiR imgui-java binding) powers the debug overlay and is exposed as api so
  // games can add their own ImGui panels through the overlay's onDrawImGui hook. Only the core
  // binding is used; the project ships its own bgfx renderer and SDL3 platform layer instead of
  // imgui-java's bundled GLFW/OpenGL backends.
  api(libs.imgui.binding)
  runtimeOnly(libs.imgui.natives.linux)
  runtimeOnly(libs.imgui.natives.macos)
  runtimeOnly(libs.imgui.natives.windows)

  implementation(libs.jetbrains.annotations)
  implementation(libs.jansi)

  compileOnly(libs.graalvm.nativeimage)
}
