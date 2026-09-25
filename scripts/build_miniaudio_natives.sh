#!/usr/bin/env bash
#
# Builds the miniaudio JNI native library for every desktop target and drops the results into
# the desktop module's bundled-native resources. Run from the repository root:
#
#   ./scripts/build_miniaudio_natives.sh
#
# Requirements:
#   - cmake 3.21+ on PATH.
#   - A C compiler for the target platform (gcc, clang, or MSVC via cmake's generator).
#   - A JDK 17+ install; set JAVA_HOME or let cmake's FindJNI probe it.
#
# Dependency versions, download URLs, and SHA-256 hashes are pinned in:
#   flixelgdx-miniaudio/src/main/native/deps.cmake
# That file is the single source of truth; this script delegates to cmake so values are
# never duplicated.
#
# Every produced library is committed so packaged games run with no extra setup. Only rebuild
# when flixelgdx-miniaudio/src/main/native/flixel_miniaudio.c or deps.cmake changes.
set -euo pipefail

NATIVE_DIR="flixelgdx-miniaudio/src/main/native"
OUT_BASE="flixelgdx-desktop/src/main/resources/org/flixelgdx/natives"
BUILD_DIR="$(mktemp -d)"
trap 'rm -rf "${BUILD_DIR}"' EXIT

if ! command -v cmake >/dev/null 2>&1; then
  echo "cmake not found; install cmake 3.21+ and re-run." >&2
  exit 1
fi

# Locate JNI headers.
if [ -z "${JAVA_HOME:-}" ]; then
  for candidate in /opt/graalvm-jdk-17 /usr/lib/jvm/java-17-openjdk-amd64 /usr/lib/jvm/default-java; do
    if [ -f "${candidate}/include/jni.h" ]; then
      JAVA_HOME="${candidate}"
      break
    fi
  done
fi
if [ -z "${JAVA_HOME:-}" ] || [ ! -f "${JAVA_HOME}/include/jni.h" ]; then
  echo "Could not find jni.h. Set JAVA_HOME to a JDK 17 install." >&2
  exit 1
fi
echo "Using JNI headers from: ${JAVA_HOME}/include"

# Determine output subdirectory for this host.
OS="$(uname -s)"
ARCH="$(uname -m)"

case "${OS}" in
  Linux)
    if [ "${ARCH}" = "x86_64" ]; then
      OUT_SUBDIR="linux-x86_64"
      LIB_NAME="libflixel_miniaudio.so"
    elif [ "${ARCH}" = "aarch64" ]; then
      OUT_SUBDIR="linux-arm64"
      LIB_NAME="libflixel_miniaudio.so"
    else
      echo "Unsupported Linux arch '${ARCH}'." >&2
      exit 1
    fi
    ;;
  Darwin)
    OUT_SUBDIR="macos"
    LIB_NAME="libflixel_miniaudio.dylib"
    ;;
  MINGW*|CYGWIN*|MSYS*)
    if [ "${ARCH}" = "x86_64" ]; then
      OUT_SUBDIR="windows-x86_64"
    else
      OUT_SUBDIR="windows-arm64"
    fi
    LIB_NAME="flixel_miniaudio.dll"
    ;;
  *)
    echo "Unrecognised OS '${OS}'." >&2
    exit 1
    ;;
esac

mkdir -p "${OUT_BASE}/${OUT_SUBDIR}"

echo "Configuring cmake (target: ${OUT_SUBDIR}) ..."

CMAKE_EXTRA=()
if [ "${OS}" = "Darwin" ]; then
  # Universal binary covering both Intel and Apple Silicon.
  CMAKE_EXTRA+=("-DCMAKE_OSX_ARCHITECTURES=x86_64;arm64")
fi

cmake -S "${NATIVE_DIR}" -B "${BUILD_DIR}" \
  -DFLIXEL_MINIAUDIO_BUILD_SHARED=ON \
  -DJAVA_HOME="${JAVA_HOME}" \
  -DCMAKE_BUILD_TYPE=Release \
  "${CMAKE_EXTRA[@]}"

echo "Building ..."
cmake --build "${BUILD_DIR}" --config Release

# Locate the output shared library. cmake names the target "flixel_miniaudio_shared" but sets
# OUTPUT_NAME "flixel_miniaudio", so the file is named as expected.
BUILT_LIB="$(find "${BUILD_DIR}" -name "${LIB_NAME}" | head -1)"
if [ -z "${BUILT_LIB}" ]; then
  echo "Build succeeded but '${LIB_NAME}' was not found in ${BUILD_DIR}." >&2
  exit 1
fi

cp "${BUILT_LIB}" "${OUT_BASE}/${OUT_SUBDIR}/${LIB_NAME}"
echo "Installed: ${OUT_BASE}/${OUT_SUBDIR}/${LIB_NAME}"
echo "Done."
