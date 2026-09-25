# deps.cmake - pinned native dependencies for flixelgdx-android
#
# This is the single source of truth for version numbers, download URLs, and SHA-256 hashes
# for the Android native build. Update this file when upgrading a dependency, then recompute
# the hash with:
#   curl -sL <URL> | sha256sum

# basis_universal 1.16.4 (https://github.com/BinomialLLC/basis_universal)
# Provides the KTX2 transcoder (transcoder/basisu_transcoder.cpp) and the Zstd single-file
# decompressor (zstd/zstddeclib.c) needed for UASTC+Zstd supercompressed textures.
#
# GitHub auto-generated tarballs embed non-deterministic metadata, so their SHA-256 digest
# varies between downloads. A shallow git clone by tag is stable and requires no hash pin.
set(FLIXEL_BASISU_VERSION     "1.16.4")
set(FLIXEL_BASISU_GIT_REPO    "https://github.com/BinomialLLC/basis_universal.git")
set(FLIXEL_BASISU_GIT_TAG     "1.16.4")
