# Bundled desktop natives

This folder holds the compiled miniaudio JNI libraries that the desktop audio backend loads at
runtime. Libraries live in per-platform subdirectories so the JAR can carry all targets at once.
`FlixelMiniAudio` detects the OS and CPU architecture at startup, extracts the matching binary
to a temp path, and loads it - packaged games need no extra setup.

| Subdirectory       | File                        | Platform              |
|--------------------|-----------------------------|-----------------------|
| `linux-x86_64/`   | `libflixel_miniaudio.so`    | Linux (x86-64)        |
| `linux-arm64/`    | `libflixel_miniaudio.so`    | Linux (AArch64)       |
| `windows-x86_64/` | `flixel_miniaudio.dll`      | Windows (x86-64)      |
| `macos/`          | `libflixel_miniaudio.dylib` | macOS (universal)     |

Rebuild with the `Build miniaudio natives` GitHub Actions workflow (`.github/workflows/build_miniaudio_natives.yml`),
or locally with `./scripts/build_miniaudio_natives.sh` from the repository root, whenever
`flixelgdx-desktop/src/main/native/flixel_miniaudio.c` changes. If you need `miniaudio.h` or
`stb_vorbis.c`, download them from the
[official miniaudio repository](https://github.com/mackron/miniaudio). They are kept out of the
framework due to their size.
