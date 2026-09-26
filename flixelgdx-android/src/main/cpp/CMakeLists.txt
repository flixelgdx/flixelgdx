# CMakeLists.txt - flixelgdx-android native build
#
# Builds the Android natives via the NDK, one library per job:
#   - libflixel_miniaudio.so: audio JNI, built from the shared flixelgdx-miniaudio module
#   - libbasisu.so: the Basis Universal KTX2 transcoder and its FlixelBasisu JNI wrapper
#
# The results are prebuilt and committed under flixelgdx-android/src/main/jniLibs, so the Gradle
# build never runs this file. Rebuild every ABI with scripts/build_android_natives.sh, which passes
# the NDK toolchain, ABI, and platform level, whenever a source file or deps.cmake changes.
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

# --- miniaudio (pulled from the sibling flixelgdx-miniaudio module) -------------------------

# Build the module's standalone shared library, named libflixel_miniaudio.so like on desktop.
# The path is relative to this file so that out-of-tree builds still resolve correctly.
set(FLIXEL_MINIAUDIO_BUILD_SHARED ON)
add_subdirectory(
  ${CMAKE_CURRENT_LIST_DIR}/../../../../flixelgdx-miniaudio/src/main/native
  ${CMAKE_CURRENT_BINARY_DIR}/miniaudio_build
)

# --- basis_universal (KTX2 transcoder only) -------------------------------------------------

FetchContent_Declare(
  basis_universal
  URL            ${FLIXEL_BASISU_URL}
  URL_HASH       SHA256=${FLIXEL_BASISU_SHA256}
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

# --- JNI shared library: libbasisu.so ------------------------------------------------------

add_library(basisu SHARED
  ${CMAKE_CURRENT_LIST_DIR}/flixel_basisu.cpp
)

target_link_libraries(basisu PRIVATE flixel_basisu_transcoder log)
