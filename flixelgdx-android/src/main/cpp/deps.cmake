# deps.cmake - pinned native dependencies for flixelgdx-android
#
# This is the single source of truth for version numbers, repositories, and commit pins
# for the Android native build. Update this file when upgrading a dependency, then recompute
# the pin with:
#   git ls-remote <repo> refs/tags/<version>

# basis_universal 1.16.4 (https://github.com/BinomialLLC/basis_universal)
# Provides the KTX2 transcoder (transcoder/basisu_transcoder.cpp) and the Zstd single-file
# decompressor (zstd/zstddeclib.c) needed for UASTC+Zstd supercompressed textures.
#
# GitHub auto-generated tarballs embed non-deterministic metadata, so their SHA-256 digest
# varies between downloads. The source is cloned by the exact commit the 1.16.4 tag points
# to instead, which pins the content just as firmly and cannot move if the tag is changed.
set(FLIXEL_BASISU_VERSION     "1.16.4")
set(FLIXEL_BASISU_GIT_REPO    "https://github.com/BinomialLLC/basis_universal.git")
set(FLIXEL_BASISU_GIT_COMMIT  "900e40fb5d2502927360fe2f31762bdbb624455f")
