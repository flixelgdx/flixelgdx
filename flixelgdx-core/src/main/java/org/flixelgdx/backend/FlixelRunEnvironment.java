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

/**
 * High-level classification of how the game's code is being loaded, used for logging and tooling.
 *
 * <p>This describes the shape of the running program (is it a loose classpath, a packaged JAR, an
 * IDE launch), which is a separate question from {@link FlixelRuntimeMode}. {@code FlixelRuntimeMode}
 * says whether the game was started in test, debug, or release; this enum says how the classes and
 * assets are physically laid out on the current platform.
 *
 * <p>Reported by {@link FlixelRuntimeDevice#getEnvironment()}. Platforms that cannot classify the
 * layout (a web browser, for example) report {@link #UNKNOWN}.
 *
 * @see FlixelRuntimeDevice#getEnvironment()
 */
public enum FlixelRunEnvironment {

  /** The game is running from an IDE launch (IntelliJ, Eclipse, and similar). */
  IDE,

  /** The game is running from a packaged distribution JAR. */
  JAR,

  /** The game is running from a plain classpath (loose class output, no packaging). */
  CLASSPATH,

  /** The game is running inside a browser tab. */
  BROWSER,

  /** The layout could not be classified, typically because the platform cannot report it. */
  UNKNOWN
}
