# deps.cmake - pinned native dependencies for flixelgdx-android
#
# The single source of truth for the version, download URL, and SHA-256 hash of each dependency of the Android
# native build. When upgrading, recompute the pin with:
#   git ls-remote <repo> refs/tags/<version>     (gives the commit)
#   curl -sL <URL> | sha256sum                   (gives the hash)

# basis_universal 1.16.4 (https://github.com/BinomialLLC/basis_universal), Apache License 2.0.
# Provides the KTX2 transcoder (transcoder/basisu_transcoder.cpp) and the single-file Zstandard decompressor
# (zstd/zstddeclib.c) that UASTC files supercompressed with Zstandard need.
#
# The archive of the exact commit the 1.16.4 tag points to is downloaded (about 29 MB), instead of cloning the
# repository, whose full history is over 400 MB.
set(FLIXEL_BASISU_VERSION  "1.16.4")
set(FLIXEL_BASISU_COMMIT   "900e40fb5d2502927360fe2f31762bdbb624455f")
set(FLIXEL_BASISU_URL      "https://github.com/BinomialLLC/basis_universal/archive/${FLIXEL_BASISU_COMMIT}.tar.gz")
set(FLIXEL_BASISU_SHA256   "c139c6d101f28f13d316be20487f99a4b1381921a1386dd1cceb5a0497d20cfa")
