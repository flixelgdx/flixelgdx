# CMakeLists.txt - flixelgdx-android native build
#
# Builds libflixelgdx.so for Android via the NDK. It bundles:
#   - flixel_miniaudio (audio JNI, a static lib from the shared miniaudio module)
#   - Basis Universal KTX2 transcoder (FlixelBasisu JNI wrapper)
#
# The Android Gradle Plugin drives this file via externalNativeBuild; it selects ABI, sysroot,
# and toolchain automatically. Do not invoke cmake directly.
cmake_minimum_required(VERSION 3.21)
project(flixelgdx C CXX)

set(CMAKE_C_STANDARD   11)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimize for size and strip dead code. AGP strips debug symbols in release builds.
add_compile_options(-Os -ffunction-sections -fdata-sections -fvisibility=hidden)
add_link_options(-Wl,--gc-sections)

# Align ELF load segments to 16 KB so the library is compatible with 64-bit Android devices
# that require 16-KB page alignment (Android 15+, arm64-v8a and x86_64 ABIs).
if(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a" OR CMAKE_ANDROID_ARCH_ABI STREQUAL "x86_64")
  add_link_options(-Wl,-z,max-page-size=16384)
endif()

include(FetchContent)
include(${CMAKE_CURRENT_LIST_DIR}/deps.cmake)

# --- miniaudio (static, pulled from the sibling flixelgdx-miniaudio module) -----------------

# The path is relative to this file so that out-of-tree builds still resolve correctly.
add_subdirectory(
  ${CMAKE_CURRENT_LIST_DIR}/../../../../flixelgdx-miniaudio/src/main/native
  ${CMAKE_CURRENT_BINARY_DIR}/miniaudio_build
)

# --- basis_universal (KTX2 transcoder only) -------------------------------------------------

FetchContent_Declare(
  basis_universal
  GIT_REPOSITORY ${FLIXEL_BASISU_GIT_REPO}
  GIT_TAG        ${FLIXEL_BASISU_GIT_TAG}
  GIT_SHALLOW    TRUE
  # Point at a nonexistent subdirectory so FetchContent_MakeAvailable() does not configure
  # basis_universal's own CMake project (it builds encoders, tools, and samples we do not need).
  SOURCE_SUBDIR  flixel-sources-only
)
FetchContent_MakeAvailable(basis_universal)

set(BASISU_SRC_DIR ${basis_universal_SOURCE_DIR})

# A minimal static library containing only the transcoder and the Zstd decompressor.
# Defines choose which GPU texture formats are compiled in; only the three formats
# FlixelBasisu exposes to Java are needed (ETC2_RGBA, ASTC_4x4_RGBA, and RGBA32).
add_library(flixel_basisu_transcoder STATIC
  ${BASISU_SRC_DIR}/transcoder/basisu_transcoder.cpp
  ${BASISU_SRC_DIR}/zstd/zstddeclib.c
)

target_include_directories(flixel_basisu_transcoder PUBLIC
  ${BASISU_SRC_DIR}/transcoder
  ${BASISU_SRC_DIR}/zstd
)

# Disable GPU formats that FlixelBasisu does not expose in order to reduce code size.
# The dependency rules inside basisu_transcoder.cpp are:
#   PVRTC2 requires ATC, ATC requires DXT5A.
# All three are disabled together, and then the other PC/console-only formats are removed too.
target_compile_definitions(flixel_basisu_transcoder PUBLIC
  BASISD_SUPPORT_KTX2=1
  BASISD_SUPPORT_KTX2_ZSTD=1
  BASISD_SUPPORT_UASTC=1
  BASISD_SUPPORT_ETC2_EAC_A8=1
  BASISD_SUPPORT_ASTC=1
  BASISD_SUPPORT_DXT1=0
  BASISD_SUPPORT_DXT5A=0
  BASISD_SUPPORT_BC7_MODE5=0
  BASISD_SUPPORT_PVRTC1=0
  BASISD_SUPPORT_ATC=0
  BASISD_SUPPORT_ETC2_EAC_RG11=0
  BASISD_SUPPORT_FXT1=0
  BASISD_SUPPORT_PVRTC2=0
)

set_target_properties(flixel_basisu_transcoder PROPERTIES POSITION_INDEPENDENT_CODE ON)

# --- JNI shared library: libflixelgdx.so ----------------------------------------------------

add_library(flixelgdx SHARED
  ${CMAKE_CURRENT_LIST_DIR}/flixel_basisu.cpp
)

# Pull in miniaudio JNI symbols wholesale. --whole-archive ensures the linker keeps every
# object file in the static library, so the Java_org_flixelgdx_backend_miniaudio_* symbols
# exported from flixel_miniaudio.c are present in the final .so even though this CMakeLists
# has no direct reference to them.
target_link_libraries(flixelgdx
  PRIVATE
    -Wl,--whole-archive flixel_miniaudio -Wl,--no-whole-archive
    flixel_basisu_transcoder
    log
)

# miniaudio dlopens AAudio and OpenSL ES at runtime (no link-time dependency needed), but
# it calls dl_open, so we need -ldl available. The Android NDK exposes dl as part of libc on
# API 21+ and it is implicitly linked, but naming it explicitly is harmless and documents intent.
target_link_libraries(flixelgdx PRIVATE dl)
