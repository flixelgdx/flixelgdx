/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.backend;

import org.flixelgdx.Flixel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes the build/runtime mode the game is running in. Set once by the launcher before
 * the game starts; the framework uses this to gate debug-only features such as the debugger panels.
 */
public enum FlixelRuntimeMode {

  /** Automated test execution (headless or CI). */
  TEST,

  /** Development/debug build (debugger panels and diagnostics are available). */
  DEBUG,

  /** Production/release build (all debug facilities are stripped or disabled). */
  RELEASE;

  /**
   * Converts text such as {@code "debug"} into a runtime mode.
   *
   * <p>Launchers use this to read the mode from a JVM property, an Android intent extra, or a web
   * URL parameter, so every platform accepts the same words. Matching ignores case and
   * surrounding whitespace. Null or blank text returns {@code fallback} silently, while
   * unrecognized text logs a warning and also returns {@code fallback}.
   *
   * <p>Example:
   *
   * <pre>{@code
   * FlixelRuntimeMode mode = FlixelRuntimeMode.parse(System.getProperty("flixel.mode"), FlixelRuntimeMode.RELEASE);
   * }</pre>
   *
   * @param value The text to convert, or {@code null}.
   * @param fallback The mode to return when the text is empty or unknown.
   * @return The matching mode, or {@code fallback}.
   */
  @NotNull
  public static FlixelRuntimeMode parse(@Nullable String value, @NotNull FlixelRuntimeMode fallback) {
    if (value == null) {
      return fallback;
    }
    String mode = value.trim();
    if (mode.isEmpty()) {
      return fallback;
    }
    if (mode.equalsIgnoreCase("debug")) {
      return DEBUG;
    }
    if (mode.equalsIgnoreCase("test")) {
      return TEST;
    }
    if (mode.equalsIgnoreCase("release")) {
      return RELEASE;
    }
    Flixel.log.warn("Unknown flixel.mode '{}'; defaulting to {}.", mode, fallback);
    return fallback;
  }
}
