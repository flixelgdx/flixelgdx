# Bundled desktop natives

This folder holds the compiled miniaudio JNI library that the desktop audio backend loads at
runtime. The libraries are committed so packaged games (JARs, native images) run with no extra
setup: `FlixelMiniAudio` extracts the matching file to a temp path and loads it.

| Kernel  | File |
|---------|------|
| Linux   | `libflixel_miniaudio.so` |
| Windows | `flixel_miniaudio.dll` |
| macOS   | `libflixel_miniaudio.dylib` |

Rebuild with `./scripts/build_miniaudio_natives.sh` from the repository root whenever
`flixelgdx-desktop/src/main/native/flixel_miniaudio.c` or `miniaudio.h` changes. The macOS slice
must be built on a Mac or with an osxcross toolchain; the Linux and Windows slices cross-compile
from Linux with gcc and mingw-w64.
