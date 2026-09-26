#!/usr/bin/env bash
#
# Builds the Android native libraries for every supported ABI and drops the results into the
# Android module's jniLibs folder, where the Android build packages them automatically. Run from
# the repository root:
#
#   ./scripts/build_android_natives.sh
#
# It produces, per ABI (arm64-v8a, armeabi-v7a, x86_64):
#   - libflixel_miniaudio.so: the audio engine, built from flixelgdx-miniaudio/src/main/native
#   - libbasisu.so: the Basis Universal KTX2 transcoder and its JNI wrapper
#
# Requirements (both install through the Android SDK manager):
#   - Android NDK 28.2.13676358. Set ANDROID_NDK_HOME, or ANDROID_HOME / ANDROID_SDK_ROOT so the
#     script can find it under ndk/.
#   - CMake 3.21+ and Ninja. The SDK's cmake;3.22.1 package ships both; any cmake and ninja on
#     PATH work too.
#
# The NDK cross-compiles, so one host (Linux, macOS, or Windows through Git Bash) builds every ABI.
# Dependency versions and SHA-256 hashes are pinned in flixelgdx-android/src/main/cpp/deps.cmake
# and flixelgdx-miniaudio/src/main/native/deps.cmake.
#
# The produced libraries are committed so building the framework or a game never needs the NDK.
# Only rebuild when flixel_basisu.cpp, flixel_miniaudio.c, a CMakeLists.txt, or a deps.cmake
# changes.
set -euo pipefail

NDK_VERSION="28.2.13676358"
MIN_SDK="24"
ABIS=("arm64-v8a" "armeabi-v7a" "x86_64")
SOURCE_DIR="flixelgdx-android/src/main/cpp"
OUT_BASE="flixelgdx-android/src/main/jniLibs"
BUILD_DIR="$(mktemp -d)"
trap 'rm -rf "${BUILD_DIR}"' EXIT

SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-${HOME}/Android/Sdk}}"
NDK_DIR="${ANDROID_NDK_HOME:-${SDK_DIR}/ndk/${NDK_VERSION}}"
if [ ! -f "${NDK_DIR}/build/cmake/android.toolchain.cmake" ]; then
  echo "Android NDK ${NDK_VERSION} not found at '${NDK_DIR}'." >&2
  echo "Install it with: sdkmanager \"ndk;${NDK_VERSION}\", or set ANDROID_NDK_HOME." >&2
  exit 1
fi

# Prefer the SDK's CMake package (it bundles Ninja), then fall back to PATH.
CMAKE="cmake"
NINJA="ninja"
SDK_CMAKE_BIN="$(ls -d "${SDK_DIR}"/cmake/*/bin 2>/dev/null | sort -V | tail -1 || true)"
if [ -n "${SDK_CMAKE_BIN}" ] && [ -x "${SDK_CMAKE_BIN}/cmake" ]; then
  CMAKE="${SDK_CMAKE_BIN}/cmake"
  NINJA="${SDK_CMAKE_BIN}/ninja"
fi
if ! command -v "${CMAKE}" >/dev/null 2>&1 || ! command -v "${NINJA}" >/dev/null 2>&1; then
  echo "cmake and ninja are required; install the SDK's cmake package or put both on PATH." >&2
  exit 1
fi

STRIP="$(ls "${NDK_DIR}"/toolchains/llvm/prebuilt/*/bin/llvm-strip* | head -1)"

for ABI in "${ABIS[@]}"; do
  echo "Building ${ABI} ..."
  "${CMAKE}" -S "${SOURCE_DIR}" -B "${BUILD_DIR}/${ABI}" -G Ninja \
    -DCMAKE_MAKE_PROGRAM="${NINJA}" \
    -DCMAKE_TOOLCHAIN_FILE="${NDK_DIR}/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="${ABI}" \
    -DANDROID_PLATFORM="android-${MIN_SDK}" \
    -DANDROID_STL=c++_static \
    -DCMAKE_BUILD_TYPE=Release
  "${CMAKE}" --build "${BUILD_DIR}/${ABI}"

  mkdir -p "${OUT_BASE}/${ABI}"
  for LIB in libflixel_miniaudio.so libbasisu.so; do
    BUILT="$(find "${BUILD_DIR}/${ABI}" -name "${LIB}" | head -1)"
    if [ -z "${BUILT}" ]; then
      echo "Build succeeded but '${LIB}' was not found for ${ABI}." >&2
      exit 1
    fi
    # Strip debug symbols so the committed libraries (and every game's APK) stay small.
    "${STRIP}" --strip-unneeded -o "${OUT_BASE}/${ABI}/${LIB}" "${BUILT}"
    echo "Installed: ${OUT_BASE}/${ABI}/${LIB}"
  done
done

echo "Done."
