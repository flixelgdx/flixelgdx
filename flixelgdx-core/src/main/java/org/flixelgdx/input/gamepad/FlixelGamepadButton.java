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
 * Identifies a gamepad button using an open, extensible ID string.
 *
 * <p>This is intentionally not an enum. The framework provides all standard buttons as constants;
 * hardware that exposes additional inputs (a touchpad click, a home button, a share button) can
 * name them with {@link #of(String)} without the framework needing to know they exist. Every ID is
 * interned, so the same string always yields the same instance and comparisons can use {@code ==}.
 *
 * <p>Two special sentinels are provided:
 * <ul>
 *   <li>{@link #ANY} - matches any button in queries that support wildcard checks.
 *   <li>{@link #NONE} - matches no button; returned by queries that find no result.
 * </ul>
 *
 * <p>Example:
 *
 * <pre>{@code
 * if (Flixel.gamepads.justPressed(0, FlixelGamepadButton.A)) {
 *   jump();
 * }
 *
 * // Custom button for a controller with a dedicated touchpad.
 * public static final FlixelGamepadButton TOUCHPAD = FlixelGamepadButton.of("Touchpad");
 * }</pre>
 *
 * @see FlixelGamepadAxis
 * @see FlixelGamepadMapping
 */
public final class FlixelGamepadButton {

  private static final FlixelMap<String, FlixelGamepadButton> REGISTRY = new FlixelMap<>();

  /** Matches no button; returned by queries that find no result. */
  public static final FlixelGamepadButton NONE = of("__none__");

  /** Matches any button in queries that support wildcard checks. */
  public static final FlixelGamepadButton ANY = of("__any__");

  /** Bottom face button (A on Xbox, Cross on PlayStation). */
  public static final FlixelGamepadButton A = of("A");

  /** Right face button (B on Xbox, Circle on PlayStation). */
  public static final FlixelGamepadButton B = of("B");

  /** Third face button, present on some controllers (C on Sega-style pads). */
  public static final FlixelGamepadButton C = of("C");

  /** Left face button (X on Xbox, Square on PlayStation). */
  public static final FlixelGamepadButton X = of("X");

  /** Top face button (Y on Xbox, Triangle on PlayStation). */
  public static final FlixelGamepadButton Y = of("Y");

  /** Sixth face button, present on some controllers (Z on Sega-style pads). */
  public static final FlixelGamepadButton Z = of("Z");

  /** Left shoulder bumper (L1 / LB). */
  public static final FlixelGamepadButton L1 = of("L1");

  /** Right shoulder bumper (R1 / RB). */
  public static final FlixelGamepadButton R1 = of("R1");

  /**
   * Left trigger as a button (L2 / LT).
   *
   * <p>On backends where the trigger is an analog axis, the manager synthesizes this button state
   * from the axis value once it passes a threshold. Use {@link FlixelGamepadAxis#L2} to read the
   * raw analog pressure instead.
   */
  public static final FlixelGamepadButton L2 = of("L2");

  /**
   * Right trigger as a button (R2 / RT).
   *
   * <p>On backends where the trigger is an analog axis, the manager synthesizes this button state
   * from the axis value once it passes a threshold. Use {@link FlixelGamepadAxis#R2} to read the
   * raw analog pressure instead.
   */
  public static final FlixelGamepadButton R2 = of("R2");

  /** Left stick click (L3 / LS). */
  public static final FlixelGamepadButton LEFT_STICK = of("LeftStick");

  /** Right stick click (R3 / RS). */
  public static final FlixelGamepadButton RIGHT_STICK = of("RightStick");

  /** Start / Options / Plus button. */
  public static final FlixelGamepadButton START = of("Start");

  /** Back / Select / Minus / Share button. */
  public static final FlixelGamepadButton BACK = of("Back");

  /** Mode button, found on some controllers (for example the Ouya). */
  public static final FlixelGamepadButton MODE = of("Mode");

  /** D-pad up direction. */
  public static final FlixelGamepadButton DPAD_UP = of("DpadUp");

  /** D-pad down direction. */
  public static final FlixelGamepadButton DPAD_DOWN = of("DpadDown");

  /** D-pad left direction. */
  public static final FlixelGamepadButton DPAD_LEFT = of("DpadLeft");

  /** D-pad right direction. */
  public static final FlixelGamepadButton DPAD_RIGHT = of("DpadRight");

  private final String id;

  private FlixelGamepadButton(String id) {
    this.id = id;
  }

  /**
   * Returns the canonical button for the given ID, creating and interning it on first use.
   *
   * <p>Calling this twice with the same ID returns the exact same instance, so results compare
   * equal with {@code ==}. Use this to define a custom button or to look one up by its ID string.
   *
   * @param id The button ID (for example {@code "Touchpad"}); must not be {@code null}.
   * @return The one shared {@link FlixelGamepadButton} for that ID.
   */
  @NotNull
  public static FlixelGamepadButton of(@NotNull String id) {
    Objects.requireNonNull(id, "The provided button ID cannot be null.");
    FlixelGamepadButton existing = REGISTRY.get(id);
    if (existing != null) {
      return existing;
    }
    FlixelGamepadButton created = new FlixelGamepadButton(id);
    REGISTRY.put(id, created);
    return created;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof FlixelGamepadButton)) {
      return false;
    }
    return id.equals(((FlixelGamepadButton) other).id);
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
   * @return The button's ID string (for example {@code "A"}); never {@code null}.
   */
  @NotNull
  public String getId() {
    return id;
  }
}
