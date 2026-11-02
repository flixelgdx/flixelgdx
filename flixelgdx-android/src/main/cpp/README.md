# Android natives

The Android backend loads two native libraries of its own, plus the WebGPU libraries that come from jwebgpu:

| Library                  | Built from                                               | Loaded by                                          |
|--------------------------|----------------------------------------------------------|----------------------------------------------------|
| `libflixel_miniaudio.so` | `flixelgdx-miniaudio/src/main/native/flixel_miniaudio.c` | `FlixelAndroidLauncher`, through `FlixelMiniAudio` |
| `libflixel_basisu.so`    | `flixel_basisu.cpp` and the Basis Universal transcoder   | `FlixelAndroidBasisTranscoder`, at launch          |
| `libwebgpu_dawn.so`      | Dawn, inside the `jwebgpu-jni` AAR                       | `JWebGpuJni.install()` in `FlixelAndroidLauncher`  |
| `libjwebgpu_jni.so`      | The JNI glue, inside the `jwebgpu-jni` AAR               | `JWebGpuJni.install()` in `FlixelAndroidLauncher`  |

`libflixel_basisu.so` transcodes the supercompressed `.ktx2` textures that the Basis Universal Gradle plugin produces
into ASTC, ETC2, BC7 or RGBA8, whichever the GPU supports best. Only those formats are compiled in, to keep the
library small.

Both libraries are prebuilt for `arm64-v8a` and `x86_64` (the only ABIs jwebgpu supports) and committed under
`flixelgdx-android/src/main/jniLibs/<abi>/`. The Android build packages that folder automatically, so building the
framework or a game never needs the NDK.

Rebuild them from the repository root whenever `flixel_miniaudio.c`, `flixel_basisu.cpp`, a `CMakeLists.txt`, or a
`deps.cmake` changes:

```bash
./scripts/build_android_natives.sh
```

The script needs Android NDK `28.2.13676358` and CMake 3.21+ with Ninja, both available from the
Android SDK manager (`ndk;28.2.13676358` and `cmake;3.22.1`). The NDK cross-compiles, so one
machine builds every ABI. Dependency versions and SHA-256 hashes are pinned in
`flixelgdx-miniaudio/src/main/native/deps.cmake` (miniaudio) and `deps.cmake` in this folder (Basis Universal).
