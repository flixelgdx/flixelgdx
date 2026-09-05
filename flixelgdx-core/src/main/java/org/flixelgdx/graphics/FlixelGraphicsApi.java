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

import java.util.Objects;

/**
 * Identifies which graphics backend is running, using an open, extensible ID string.
 *
 * <p>This is intentionally not an enum. An enum is a fixed, closed set: it can only ever name the
 * backends the framework hard-codes, so a power user plugging in their own renderer could never
 * identify it. An ID-string identity has no such ceiling. The framework provides the common
 * backends as constants, and anyone can mint a new one with {@link #of(String)} without the
 * framework needing to know it exists.
 *
 * <p>Game code rarely needs this; it exists mostly for introspection, such as logging what is
 * running or showing it on a debug overlay. The underlying drawing library is never exposed to game
 * code directly. Read the current backend from {@link FlixelGraphicsManager#getApi()}.
 *
 * <p>Every ID is interned, so the same ID always yields the same instance, and you can compare with
 * {@code ==}:
 *
 * <pre>{@code
 * if (Flixel.graphics.getApi() == FlixelGraphicsApi.Vulkan) {
 *   // Enable a Vulkan-specific rendering path.
 * }
 * }</pre>
 *
 * @see FlixelGraphicsManager#getApi()
 */
public final class FlixelGraphicsApi {

  private static final FlixelMap<String, FlixelGraphicsApi> REGISTRY = new FlixelMap<>();

  /** The desktop OpenGL renderer. */
  public static final FlixelGraphicsApi OpenGL = of("OpenGL");

  /** The embedded or mobile OpenGL ES renderer. */
  public static final FlixelGraphicsApi OpenGLES = of("OpenGLES");

  /** The cross-platform Vulkan renderer. */
  public static final FlixelGraphicsApi Vulkan = of("Vulkan");

  /** The Apple Metal renderer, used on macOS and iOS. */
  public static final FlixelGraphicsApi Metal = of("Metal");

  /** The Direct3D 11 renderer, used on Windows. */
  public static final FlixelGraphicsApi Direct3D11 = of("Direct3D11");

  /** The Direct3D 12 renderer, used on Windows. */
  public static final FlixelGraphicsApi Direct3D12 = of("Direct3D12");

  /** The web backend built on the browser's native WebGPU. */
  public static final FlixelGraphicsApi WebGPU = of("WebGPU");

  /** The web backend built on WebGL. */
  public static final FlixelGraphicsApi WebGL = of("WebGL");

  /**
   * No real backend is present.
   *
   * <p>This is reported by the safe default manager on headless or not-yet-initialized sessions,
   * where drawing is a no-op.
   */
  public static final FlixelGraphicsApi Noop = of("Noop");

  private final String id;

  private FlixelGraphicsApi(String id) {
    this.id = id;
  }

  /**
   * Returns the canonical backend type for the given ID, creating and interning it on first use.
   *
   * <p>Calling this twice with the same ID returns the very same instance, so results compare equal
   * with {@code ==}. Use this to define a custom backend's identity, or to look one up by ID.
   *
   * @param id The backend ID, for example {@code "bgfx"}; must not be {@code null}.
   * @return The one shared {@link FlixelGraphicsApi} for that id.
   */
  @NotNull
  public static FlixelGraphicsApi of(@NotNull String id) {
    Objects.requireNonNull(id, "The ID for the provided graphics API cannot be null.");
    FlixelGraphicsApi existing = REGISTRY.get(id);
    if (existing != null) {
      return existing;
    }
    FlixelGraphicsApi created = new FlixelGraphicsApi(id);
    REGISTRY.put(id, created);
    return created;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof FlixelGraphicsApi)) {
      return false;
    }
    return id.equals(((FlixelGraphicsApi) other).id);
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
   * Returns the backend's ID string (for example, {@code "bgfx"}); never {@code null}.
   *
   * @return The graphics API ID string, never {@code null}.
   */
  @NotNull
  public String getId() {
    return id;
  }
}
