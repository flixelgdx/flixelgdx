package me.stringdotjar.flixelgdx.backend.runtime;

/**
 * Describes the build/runtime mode the game is running in. Set once by the launcher before
 * the game starts; the framework uses this to gate debug-only features such as the debug overlay.
 */
public enum FlixelRuntimeMode {

  /** Automated test execution (headless or CI). */
  TEST,

  /** Development/debug build (debug overlay and diagnostics are available). */
  DEBUG,

  /** Production/release build (all debug facilities are stripped or disabled). */
  RELEASE
}
