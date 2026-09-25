# deps.cmake - pinned native dependencies for flixelgdx-android
#
# This is the single source of truth for version numbers, download URLs, and SHA-256 hashes
# for the Android native build. Update this file when upgrading a dependency, then recompute
# the pin with:
#   git ls-remote <repo> refs/tags/<version>     (gives the commit)
#   curl -sL <URL> | sha256sum                   (gives the hash)

# basis_universal 1.16.4 (https://github.com/BinomialLLC/basis_universal)
# Provides the KTX2 transcoder (transcoder/basisu_transcoder.cpp) and the Zstd single-file
# decompressor (zstd/zstddeclib.c) needed for UASTC+Zstd supercompressed textures.
#
# The archive is downloaded by the exact commit the 1.16.4 tag points to (about 29 MB), rather
# than cloning the repository, whose full history is over 400 MB.
set(FLIXEL_BASISU_VERSION  "1.16.4")
set(FLIXEL_BASISU_COMMIT   "900e40fb5d2502927360fe2f31762bdbb624455f")
set(FLIXEL_BASISU_URL      "https://github.com/BinomialLLC/basis_universal/archive/${FLIXEL_BASISU_COMMIT}.tar.gz")
set(FLIXEL_BASISU_SHA256   "c139c6d101f28f13d316be20487f99a4b1381921a1386dd1cceb5a0497d20cfa")
