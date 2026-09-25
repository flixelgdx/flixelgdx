# CMakeLists.txt - flixelgdx-miniaudio native build
#
# Builds flixel_miniaudio as a reusable STATIC library with POSITION_INDEPENDENT_CODE ON.
# An Android sub-project links this static target into its own JNI shared library:
#
#   add_subdirectory(path/to/flixelgdx-miniaudio/src/main/native miniaudio_build)
#   target_link_libraries(my_jni_lib PRIVATE flixel_miniaudio)
#
# To build a standalone shared library for desktop (output: libflixel_miniaudio.so etc.),
# pass -DFLIXEL_MINIAUDIO_BUILD_SHARED=ON to cmake.
#
# Dependencies are fetched automatically via CMake FetchContent; cmake 3.21+ and a C compiler
# are the only build requirements. Set JAVA_HOME to a JDK 17+ install for JNI headers, or
# let FindJNI locate them automatically.
cmake_minimum_required(VERSION 3.21)
project(flixel_miniaudio C)

include(FetchContent)
include(${CMAKE_CURRENT_LIST_DIR}/deps.cmake)

# --- Fetch miniaudio ----------------------------------------------------------

FetchContent_Declare(
  miniaudio
  URL      ${FLIXEL_MINIAUDIO_URL}
  URL_HASH SHA256=${FLIXEL_MINIAUDIO_SHA256}
)
FetchContent_MakeAvailable(miniaudio)

# miniaudio.h lives at the root of the archive; stb_vorbis.c is under extras/.
set(MINIAUDIO_INCLUDE_DIR    ${miniaudio_SOURCE_DIR})
set(MINIAUDIO_EXTRAS_DIR     ${miniaudio_SOURCE_DIR}/extras)

# --- JNI headers --------------------------------------------------------------

find_package(JNI REQUIRED)

# --- Static library (primary reusable target) ---------------------------------

add_library(flixel_miniaudio STATIC ${CMAKE_CURRENT_LIST_DIR}/flixel_miniaudio.c)
set_target_properties(flixel_miniaudio PROPERTIES POSITION_INDEPENDENT_CODE ON)

target_include_directories(flixel_miniaudio PUBLIC
  ${MINIAUDIO_INCLUDE_DIR}
  ${MINIAUDIO_EXTRAS_DIR}
  ${JNI_INCLUDE_DIRS}
)

# Platform link requirements expected by miniaudio.
if(WIN32)
  target_link_libraries(flixel_miniaudio PUBLIC ole32 winmm)
elseif(APPLE)
  target_link_libraries(flixel_miniaudio PUBLIC
    "-framework CoreAudio"
    "-framework AudioToolbox"
    "-framework AudioUnit"
    "-framework CoreServices"
  )
else()
  # Linux / Android
  target_link_libraries(flixel_miniaudio PUBLIC m)
  if(NOT ANDROID)
    target_link_libraries(flixel_miniaudio PUBLIC pthread dl)
  endif()
endif()

# --- Optional standalone shared library (for desktop) -------------------------

option(FLIXEL_MINIAUDIO_BUILD_SHARED "Build a standalone shared library for desktop" OFF)

if(FLIXEL_MINIAUDIO_BUILD_SHARED)
  add_library(flixel_miniaudio_shared SHARED ${CMAKE_CURRENT_LIST_DIR}/flixel_miniaudio.c)
  target_include_directories(flixel_miniaudio_shared PRIVATE
    ${MINIAUDIO_INCLUDE_DIR}
    ${MINIAUDIO_EXTRAS_DIR}
    ${JNI_INCLUDE_DIRS}
  )
  if(WIN32)
    # Static CRT so the DLL has no MSVC/MinGW runtime dependency.
    set_target_properties(flixel_miniaudio_shared PROPERTIES
      MSVC_RUNTIME_LIBRARY "MultiThreaded"
    )
    target_link_options(flixel_miniaudio_shared PRIVATE -static-libgcc)
    target_link_libraries(flixel_miniaudio_shared PRIVATE ole32 winmm)
    set_target_properties(flixel_miniaudio_shared PROPERTIES OUTPUT_NAME "flixel_miniaudio")
  elseif(APPLE)
    target_link_libraries(flixel_miniaudio_shared PRIVATE
      "-framework CoreAudio"
      "-framework AudioToolbox"
      "-framework AudioUnit"
      "-framework CoreServices"
    )
    set_target_properties(flixel_miniaudio_shared PROPERTIES OUTPUT_NAME "flixel_miniaudio")
  else()
    target_link_libraries(flixel_miniaudio_shared PRIVATE m pthread dl)
    set_target_properties(flixel_miniaudio_shared PROPERTIES OUTPUT_NAME "flixel_miniaudio")
  endif()
endif()
