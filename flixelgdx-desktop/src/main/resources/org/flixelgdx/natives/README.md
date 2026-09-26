# Bundled desktop natives

This folder holds the compiled miniaudio JNI libraries that the desktop audio backend loads at
runtime. Libraries live in per-platform subdirectories so the JAR can carry all targets at once.
`FlixelDesktopMiniAudioLoader` detects the OS and CPU architecture at startup, extracts the
matching binary to a temp path, and loads it - packaged games need no extra setup.

| Subdirectory       | File                        | Platform              |
|--------------------|-----------------------------|-----------------------|
| `linux-x86_64/`    | `libflixel_miniaudio.so`    | Linux (x86-64)        |
| `linux-arm64/`     | `libflixel_miniaudio.so`    | Linux (AArch64)       |
| `windows-x86_64/`  | `flixel_miniaudio.dll`      | Windows (x86-64)      |
| `windows-arm64/`   | `flixel_miniaudio.dll`      | Windows (AArch64)     |
| `macos/`           | `libflixel_miniaudio.dylib` | macOS (universal)     |

Rebuild with the `Build miniaudio natives` GitHub Actions workflow
(`.github/workflows/build_miniaudio_natives.yml`), or locally with
`./scripts/build_miniaudio_natives.sh` from the repository root, whenever
`flixelgdx-miniaudio/src/main/native/flixel_miniaudio.c` changes.

Dependency versions and SHA-256 hashes are pinned in
`flixelgdx-miniaudio/src/main/native/deps.cmake`, which is the single source of truth for
the build. The CMakeLists.txt fetches miniaudio (including its bundled `extras/stb_vorbis.c`)
automatically, so no manual header downloads are needed.
