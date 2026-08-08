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
package org.flixelgdx.input.gamepad;

import org.flixelgdx.collections.FlixelMap;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Identifies a gamepad analog axis using an open, extensible ID string.
 *
 * <p>This is intentionally not an enum. The framework provides the standard stick and trigger axes
 * as constants; hardware that exposes additional axes (a pressure-sensitive touchpad, a gyroscope
 * channel) can name them with {@link #of(String)} without the framework needing to know they exist.
 * Every ID is interned, so the same string always yields the same instance and comparisons can use
 * {@code ==}.
 *
 * <p>Axis values are normally in the range {@code [-1, 1]}. Triggers ({@link #L2} and {@link #R2})
 * may range from {@code [0, 1]} depending on the backend.
 *
 * <p>Example:
 *
 * <pre>{@code
 * float tiltX = Flixel.gamepads.getAxis(0, FlixelGamepadAxis.LEFT_X);
 * player.velocityX = tiltX * speed;
 * }</pre>
 *
 * @see FlixelGamepadButton
 * @see FlixelGamepadMapping
 */
public final class FlixelGamepadAxis {

  private static final FlixelMap<String, FlixelGamepadAxis> REGISTRY = new FlixelMap<>();

  /** Horizontal axis of the left analog stick. Negative = left, positive = right. */
  public static final FlixelGamepadAxis LEFT_X = of("LeftX");

  /** Vertical axis of the left analog stick. Negative = up, positive = down (backend-dependent). */
  public static final FlixelGamepadAxis LEFT_Y = of("LeftY");

  /** Horizontal axis of the right analog stick. Negative = left, positive = right. */
  public static final FlixelGamepadAxis RIGHT_X = of("RightX");

  /** Vertical axis of the right analog stick. Negative = up, positive = down (backend-dependent). */
  public static final FlixelGamepadAxis RIGHT_Y = of("RightY");

  /**
   * Left trigger as an analog axis (L2 / LT).
   *
   * <p>On backends where the trigger is a digital button, this axis is not registered in the
   * mapping and returns {@code 0} when queried. Use {@link FlixelGamepadButton#L2} for the
   * boolean pressed state.
   */
  public static final FlixelGamepadAxis L2 = of("L2");

  /**
   * Right trigger as an analog axis (R2 / RT).
   *
   * <p>On backends where the trigger is a digital button, this axis is not registered in the
   * mapping and returns {@code 0} when queried. Use {@link FlixelGamepadButton#R2} for the
   * boolean pressed state.
   */
  public static final FlixelGamepadAxis R2 = of("R2");

  private final String id;

  private FlixelGamepadAxis(String id) {
    this.id = id;
  }

  /**
   * Returns the canonical axis for the given ID, creating and interning it on first use.
   *
   * <p>Calling this twice with the same ID returns the exact same instance, so results compare
   * equal with {@code ==}. Use this to define a custom axis or to look one up by its ID string.
   *
   * @param id The axis ID (for example {@code "GyroX"}); must not be {@code null}.
   * @return The one shared {@link FlixelGamepadAxis} for that ID.
   */
  @NotNull
  public static FlixelGamepadAxis of(@NotNull String id) {
    Objects.requireNonNull(id, "The provided axis ID cannot be null.");
    FlixelGamepadAxis existing = REGISTRY.get(id);
    if (existing != null) {
      return existing;
    }
    FlixelGamepadAxis created = new FlixelGamepadAxis(id);
    REGISTRY.put(id, created);
    return created;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof FlixelGamepadAxis)) {
      return false;
    }
    return id.equals(((FlixelGamepadAxis) other).id);
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
   * @return The axis's ID string (for example {@code "LeftX"}); never {@code null}.
   */
  @NotNull
  public String getId() {
    return id;
  }
}
