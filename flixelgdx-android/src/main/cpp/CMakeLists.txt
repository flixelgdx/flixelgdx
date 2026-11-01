# CMakeLists.txt - flixelgdx-android native build
#
# Builds the Android natives via the NDK:
#   - libflixel_miniaudio.so: audio JNI, built from the shared flixelgdx-miniaudio module
#
# The results are prebuilt and committed under flixelgdx-android/src/main/jniLibs, so the Gradle
# build never runs this file. Rebuild every ABI with scripts/build_android_natives.sh, which passes
# the NDK toolchain, ABI, and platform level, whenever a source file or the miniaudio deps.cmake changes.
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


# --- miniaudio (pulled from the sibling flixelgdx-miniaudio module) -------------------------

# Build the module's standalone shared library, named libflixel_miniaudio.so like on desktop.
# The path is relative to this file so that out-of-tree builds still resolve correctly.
set(FLIXEL_MINIAUDIO_BUILD_SHARED ON)
add_subdirectory(
  ${CMAKE_CURRENT_LIST_DIR}/../../../../flixelgdx-miniaudio/src/main/native
  ${CMAKE_CURRENT_BINARY_DIR}/miniaudio_build
)
