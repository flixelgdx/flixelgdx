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

  implementation(libs.jetbrains.annotations)
  implementation(libs.jansi)

  compileOnly(libs.graalvm.nativeimage)
}
