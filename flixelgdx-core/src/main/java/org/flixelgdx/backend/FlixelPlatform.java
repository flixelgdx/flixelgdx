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

import org.flixelgdx.collections.FlixelMap;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies the platform a game is running on, using an open, extensible id string.
 *
 * <p>This is intentionally not an enum. An enum is a fixed, closed set: it can only ever hold the
 * values the framework hard-codes, so a power user shipping their own platform (a console port, an
 * embedded device, an experimental target) could never name it. An id-string identity has no such
 * ceiling. The framework provides the common platforms as constants, and anyone can mint a new one
 * with {@link #of(String)} without the framework needing to know it exists.
 *
 * <p>Read the current platform from {@link FlixelHostIntegration#getPlatform() Flixel.host.getPlatform()}.
 * Every id is interned, so the same id always yields the exact same instance and you can compare
 * with {@code ==}:
 *
 * <pre>{@code
 * if (Flixel.host.getPlatform() == FlixelPlatform.Desktop) {
 *   // Desktop-only behavior.
 * }
 * }</pre>
 *
 * <p>To define a custom platform, hold a constant your backend returns:
 *
 * <pre>{@code
 * public static final FlixelPlatform MY_CONSOLE = FlixelPlatform.of("MyConsole");
 * }</pre>
 *
 * @see FlixelHostIntegration#getPlatform()
 */
public final class FlixelPlatform {

  // Declared before the constants below so their of(...) calls can register into it.
  private static final FlixelMap<String, FlixelPlatform> REGISTRY = new FlixelMap<>();

  /** The platform is not known, typically because no host integration has been installed yet. */
  public static final FlixelPlatform Unknown = of("Unknown");

  /** A desktop computer (Windows, macOS, or Linux). */
  public static final FlixelPlatform Desktop = of("Desktop");

  /** An Android device. */
  public static final FlixelPlatform Android = of("Android");

  /** A web browser. */
  public static final FlixelPlatform Web = of("Web");

  /** An iOS device. */
  public static final FlixelPlatform Ios = of("iOS");

  private final String id;

  private FlixelPlatform(String id) {
    this.id = id;
  }

  /**
   * Returns the canonical platform for the given id, creating and interning it on first use.
   *
   * <p>Calling this twice with the same id returns the very same instance, so results compare equal
   * with {@code ==}. Use this to define a custom platform, or to look one up by id.
   *
   * @param id The platform id, for example {@code "Desktop"}; must not be {@code null}.
   * @return The one shared {@link FlixelPlatform} for that id.
   */
  @NotNull
  public static FlixelPlatform of(@NotNull String id) {
    FlixelPlatform existing = REGISTRY.get(id);
    if (existing != null) {
      return existing;
    }
    FlixelPlatform created = new FlixelPlatform(id);
    REGISTRY.put(id, created);
    return created;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof FlixelPlatform)) {
      return false;
    }
    return id.equals(((FlixelPlatform) other).id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public String toString() {
    return id;
  }

  /**
   * @return The platform's id string (for example, {@code "Desktop"}); never {@code null}.
   */
  @NotNull
  public String getId() {
    return id;
  }
}
