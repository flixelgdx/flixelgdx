#!/usr/bin/env bash
#
# Builds the miniaudio JNI native library for every desktop kernel and drops the results into the
# desktop module's bundled-native resources. Run from the repository root:
#
#   ./scripts/build_miniaudio_natives.sh
#
# Requirements:
#   - A JDK with JNI headers (set JAVA_HOME, or the script probes a few common locations).
#   - Linux:   gcc
#   - Windows: x86_64-w64-mingw32-gcc (mingw-w64)
#   - macOS:   clang (native on a Mac) or an osxcross toolchain (o64-clang / oa64-clang)
#
# Every produced library is committed so packaged games run with no extra setup. Only rebuild when
# flixel_miniaudio.c or miniaudio.h changes.
set -euo pipefail

NATIVE_DIR="flixelgdx-desktop/src/main/native"
OUT_DIR="flixelgdx-desktop/src/main/resources/org/flixelgdx/natives"
SRC="${NATIVE_DIR}/flixel_miniaudio.c"

mkdir -p "${OUT_DIR}"

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
JNI_INC="${JAVA_HOME}/include"
echo "Using JNI headers from: ${JNI_INC}"

# --- Linux ---
if command -v gcc >/dev/null 2>&1; then
  echo "Building Linux (libflixel_miniaudio.so) ..."
  gcc -O2 -fPIC -shared \
    -I"${JNI_INC}" -I"${JNI_INC}/linux" \
    -o "${OUT_DIR}/libflixel_miniaudio.so" "${SRC}" \
    -lm -lpthread -ldl
else
  echo "Skipping Linux: gcc not found." >&2
fi

# --- Windows ---
if command -v x86_64-w64-mingw32-gcc >/dev/null 2>&1; then
  echo "Building Windows (flixel_miniaudio.dll) ..."
  # mingw does not ship the Windows jni_md.h; supply the standard minimal one.
  WIN_JNI="$(mktemp -d)"
  cat > "${WIN_JNI}/jni_md.h" <<'HEADER'
#ifndef _JAVASOFT_JNI_MD_H_
#define _JAVASOFT_JNI_MD_H_
#define JNIEXPORT __declspec(dllexport)
#define JNIIMPORT __declspec(dllimport)
#define JNICALL __stdcall
typedef long jint;
typedef long long jlong;
typedef signed char jbyte;
#endif
HEADER
  x86_64-w64-mingw32-gcc -O2 -shared \
    -I"${JNI_INC}" -I"${WIN_JNI}" \
    -o "${OUT_DIR}/flixel_miniaudio.dll" "${SRC}" \
    -lole32 -lwinmm -static-libgcc
  rm -rf "${WIN_JNI}"
else
  echo "Skipping Windows: x86_64-w64-mingw32-gcc not found." >&2
fi

# --- macOS ---
# Native clang on a Mac uses "${JNI_INC}/darwin". An osxcross build uses o64-clang / oa64-clang and
# the target JDK's darwin headers. Universal binary via lipo when both arch slices build.
build_macos_slice() {
  local compiler="$1" arch="$2" out="$3"
  if command -v "${compiler}" >/dev/null 2>&1; then
    echo "Building macOS ${arch} slice ..."
    "${compiler}" -O2 -dynamiclib -arch "${arch}" \
      -I"${JNI_INC}" -I"${JNI_INC}/darwin" \
      -o "${out}" "${SRC}"
    return 0
  fi
  return 1
}

MAC_X64="$(mktemp -u).dylib"
MAC_ARM="$(mktemp -u).dylib"
BUILT_X64=0
BUILT_ARM=0
if build_macos_slice "o64-clang" "x86_64" "${MAC_X64}"; then BUILT_X64=1; fi
if build_macos_slice "oa64-clang" "arm64" "${MAC_ARM}"; then BUILT_ARM=1; fi
# Native macOS fallback (running this script on a Mac).
if [ "${BUILT_X64}${BUILT_ARM}" = "00" ] && command -v clang >/dev/null 2>&1 && [ -d "${JNI_INC}/darwin" ]; then
  echo "Building macOS (native clang) ..."
  clang -O2 -dynamiclib \
    -I"${JNI_INC}" -I"${JNI_INC}/darwin" \
    -o "${OUT_DIR}/libflixel_miniaudio.dylib" "${SRC}"
elif [ "${BUILT_X64}" = "1" ] || [ "${BUILT_ARM}" = "1" ]; then
  SLICES=()
  [ "${BUILT_X64}" = "1" ] && SLICES+=("${MAC_X64}")
  [ "${BUILT_ARM}" = "1" ] && SLICES+=("${MAC_ARM}")
  if command -v lipo >/dev/null 2>&1 && [ "${#SLICES[@]}" -gt 1 ]; then
    lipo -create "${SLICES[@]}" -output "${OUT_DIR}/libflixel_miniaudio.dylib"
  else
    cp "${SLICES[0]}" "${OUT_DIR}/libflixel_miniaudio.dylib"
  fi
  rm -f "${MAC_X64}" "${MAC_ARM}"
else
  echo "Skipping macOS: no clang / osxcross toolchain found." >&2
fi

echo "Done. Built natives in ${OUT_DIR}:"
ls -la "${OUT_DIR}"
