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
package org.flixelgdx.graphics;

import org.flixelgdx.collections.FlixelMap;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies which graphics backend is running, using an open, extensible id string.
 *
 * <p>This is intentionally not an enum. An enum is a fixed, closed set: it can only ever name the
 * backends the framework hard-codes, so a power user plugging in their own renderer could never
 * identify it. An id-string identity has no such ceiling. The framework provides the common
 * backends as constants, and anyone can mint a new one with {@link #of(String)} without the
 * framework needing to know it exists.
 *
 * <p>Game code rarely needs this; it exists mostly for introspection, such as logging what is
 * running or showing it on a debug overlay. The actual drawing library stays behind the internal
 * {@link FlixelGraphicsBackend} seam and is never exposed to game code directly. Read the current
 * backend from {@link FlixelGraphicsManager#getBackendType() Flixel.graphics.getBackendType()}.
 *
 * <p>Every id is interned, so the same id always yields the same instance and you can compare with
 * {@code ==}:
 *
 * <pre>{@code
 * if (Flixel.graphics.getBackendType() == FlixelBackendType.WebGl) {
 *   // Fall back to a simpler effect on the WebGL path.
 * }
 * }</pre>
 *
 * @see FlixelGraphicsManager#getBackendType()
 */
public final class FlixelBackendType {

  // Declared before the constants below so their of(...) calls can register into it.
  private static final FlixelMap<String, FlixelBackendType> REGISTRY = new FlixelMap<>();

  /**
   * No real backend is present.
   *
   * <p>This is reported by the safe default manager on headless or not-yet-initialized sessions,
   * where drawing is a no-op.
   */
  public static final FlixelBackendType Noop = of("Noop");

  /** The native backend built on bgfx. */
  public static final FlixelBackendType Bgfx = of("bgfx");

  /** The web backend built on the browser's native WebGPU. */
  public static final FlixelBackendType WebGpu = of("WebGPU");

  /** The web backend built on WebGL. */
  public static final FlixelBackendType WebGl = of("WebGL");

  private final String id;

  private FlixelBackendType(String id) {
    this.id = id;
  }

  /**
   * Returns the canonical backend type for the given id, creating and interning it on first use.
   *
   * <p>Calling this twice with the same id returns the very same instance, so results compare equal
   * with {@code ==}. Use this to define a custom backend's identity, or to look one up by id.
   *
   * @param id The backend id, for example {@code "bgfx"}; must not be {@code null}.
   * @return The one shared {@link FlixelBackendType} for that id.
   */
  @NotNull
  public static FlixelBackendType of(@NotNull String id) {
    FlixelBackendType existing = REGISTRY.get(id);
    if (existing != null) {
      return existing;
    }
    FlixelBackendType created = new FlixelBackendType(id);
    REGISTRY.put(id, created);
    return created;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof FlixelBackendType)) {
      return false;
    }
    return id.equals(((FlixelBackendType) other).id);
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
   * @return The backend's id string (for example, {@code "bgfx"}); never {@code null}.
   */
  @NotNull
  public String getId() {
    return id;
  }
}
