# Android natives

The Android backend loads one native library of its own, plus the WebGPU libraries that come from jwebgpu:

| Library                  | Built from                                               | Loaded by                                          |
|--------------------------|----------------------------------------------------------|----------------------------------------------------|
| `libflixel_miniaudio.so` | `flixelgdx-miniaudio/src/main/native/flixel_miniaudio.c` | `FlixelAndroidLauncher`, through `FlixelMiniAudio` |
| `libwebgpu_dawn.so`      | Dawn, inside the `jwebgpu-jni` AAR                       | `JWebGpuJni.install()` in `FlixelAndroidLauncher`  |
| `libjwebgpu_jni.so`      | The JNI glue, inside the `jwebgpu-jni` AAR               | `JWebGpuJni.install()` in `FlixelAndroidLauncher`  |

`libflixel_miniaudio.so` is prebuilt for `arm64-v8a` and `x86_64` (the only ABIs jwebgpu supports) and committed under
`flixelgdx-android/src/main/jniLibs/<abi>/`. The Android build packages that folder automatically, so building the
framework or a game never needs the NDK.

Rebuild it from the repository root whenever `flixel_miniaudio.c`, the `CMakeLists.txt` files, or the miniaudio
`deps.cmake` changes:

```bash
./scripts/build_android_natives.sh
```

The script needs Android NDK `28.2.13676358` and CMake 3.21+ with Ninja, both available from the
Android SDK manager (`ndk;28.2.13676358` and `cmake;3.22.1`). The NDK cross-compiles, so one
machine builds every ABI. Dependency versions and SHA-256 hashes are pinned in
`flixelgdx-miniaudio/src/main/native/deps.cmake`.
