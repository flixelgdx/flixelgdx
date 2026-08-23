# Bundled desktop natives

This folder holds the compiled miniaudio JNI library that the desktop audio backend loads at
runtime. The libraries are committed so packaged games (JARs, native images) run with no extra
setup: `FlixelMiniAudio` extracts the matching file to a temp path and loads it.

| Kernel  | File                        |
|---------|-----------------------------|
| Linux   | `libflixel_miniaudio.so`    |
| Windows | `flixel_miniaudio.dll`      |
| macOS   | `libflixel_miniaudio.dylib` |

Rebuild with `./scripts/build_miniaudio_natives.sh` from the repository root whenever
`flixelgdx-desktop/src/main/native/flixel_miniaudio.c` or `miniaudio.h` changes. If you need
to use the `miniaudio.h` or `stb_vorbis.c` files, download them from the 
[official miniaudio repository](https://github.com/mackron/miniaudio). They are kept out of the
framework due to the sheer sizes of both of the files.
