# Android natives

The Android backend loads two native libraries, one per job:

| Library                  | Built from                                                              | Loaded by                                             |
|--------------------------|-------------------------------------------------------------------------|-------------------------------------------------------|
| `libflixel_miniaudio.so` | `flixelgdx-miniaudio/src/main/native/flixel_miniaudio.c`                | `FlixelAndroidLauncher`, through `FlixelMiniAudio`    |
| `libbasisu.so`           | `flixel_basisu.cpp` in this folder, plus the Basis Universal transcoder | `FlixelBasisu`, the first time a KTX2 texture loads   |

Both are prebuilt for `arm64-v8a`, `armeabi-v7a`, and `x86_64`, and committed under
`flixelgdx-android/src/main/jniLibs/<abi>/`. The Android build packages that folder
automatically, so building the framework or a game never needs the NDK.

Rebuild them from the repository root whenever `flixel_basisu.cpp`, `flixel_miniaudio.c`, either
`CMakeLists.txt`, or either `deps.cmake` changes:

```bash
./scripts/build_android_natives.sh
```

The script needs Android NDK `28.2.13676358` and CMake 3.21+ with Ninja, both available from the
Android SDK manager (`ndk;28.2.13676358` and `cmake;3.22.1`). The NDK cross-compiles, so one
machine builds every ABI. Dependency versions and SHA-256 hashes are pinned in
`deps.cmake` here and in `flixelgdx-miniaudio/src/main/native/deps.cmake`.
