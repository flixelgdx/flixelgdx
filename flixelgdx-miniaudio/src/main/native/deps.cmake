# deps.cmake - pinned native dependencies for flixelgdx-miniaudio
#
# This is the single source of truth for version numbers, download URLs, and SHA-256 hashes.
# CMakeLists.txt and scripts/build_miniaudio_natives.sh both reference the values set here.
# Update this file when upgrading a dependency, then recompute the hash with:
#   curl -sL <URL> | sha256sum

# miniaudio 0.11.25 (https://github.com/mackron/miniaudio)
# Provides miniaudio.h and extras/stb_vorbis.c.
set(FLIXEL_MINIAUDIO_VERSION   "0.11.25")
set(FLIXEL_MINIAUDIO_URL       "https://github.com/mackron/miniaudio/archive/refs/tags/0.11.25.tar.gz")
set(FLIXEL_MINIAUDIO_SHA256    "b900edcffe979816e2560a0580b9b1216d674b4f17fbadeca8f777a7f8ab0274")
