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
package org.flixelgdx.input.action;

import org.flixelgdx.input.gamepad.FlixelGamepadInput;
import org.jetbrains.annotations.NotNull;

/**
 * One contributor to a {@link FlixelActionAnalog} vector. Combine several bindings: WASD-style
 * {@link Kind#KEY_NEG_X}, {@link Kind#KEY_POS_X}, and stick channels {@link Kind#GAMEPAD_AXIS_X} /
 * {@link Kind#GAMEPAD_AXIS_Y} for the same physical stick on a slot.
 *
 * <p>Create bindings only during setup. Gamepad axes use {@link org.flixelgdx.input.gamepad.FlixelGamepadInput FlixelGamepadInput}
 * logical constants; {@link org.flixelgdx.Flixel#gamepads Flixel.gamepads} applies the same dead zone as the rest of a game.
 *
 * <h2>Axis conventions and Y correction</h2>
 *
 * <p>Most gamepad drivers report the stick Y axis in screen-space: pushing the stick up produces a
 * negative raw value, pushing down produces a positive raw value. This is the opposite of the
 * math-space convention used by keyboard key bindings (pressing the UP key contributes +1 to Y).
 * {@link #gamepadAxisY(int, int)} corrects this by default so the stick and keyboard share the same
 * coordinate system (up = positive Y, down = negative Y). Pass {@code raw = true} to the overload
 * {@link #gamepadAxisY(int, int, boolean)} when you specifically need the uncorrected hardware value.
 */
public final class FlixelAnalogAxisBinding {

  public final Kind kind;

  /** Keycode for key kinds, or {@link FlixelGamepadInput} logical axis for gamepad kinds. */
  public final int keyOrAxis;

  /** Gamepad slot for gamepad kinds; ignored for key kinds. */
  public final int gamepadSlot;

  private FlixelAnalogAxisBinding(Kind kind, int keyOrAxis, int gamepadSlot) {
    this.kind = kind;
    this.keyOrAxis = keyOrAxis;
    this.gamepadSlot = gamepadSlot;
  }

  @NotNull
  public static FlixelAnalogAxisBinding negXKey(int keycode) {
    return new FlixelAnalogAxisBinding(Kind.KEY_NEG_X, keycode, 0);
  }

  @NotNull
  public static FlixelAnalogAxisBinding posXKey(int keycode) {
    return new FlixelAnalogAxisBinding(Kind.KEY_POS_X, keycode, 0);
  }

  @NotNull
  public static FlixelAnalogAxisBinding negYKey(int keycode) {
    return new FlixelAnalogAxisBinding(Kind.KEY_NEG_Y, keycode, 0);
  }

  @NotNull
  public static FlixelAnalogAxisBinding posYKey(int keycode) {
    return new FlixelAnalogAxisBinding(Kind.KEY_POS_Y, keycode, 0);
  }

  /**
   * Adds the X component of a stick. Most drivers report X in math-space already (left = negative,
   * right = positive), so no correction is applied.
   *
   * @param gamepadSlot {@code 0..} slot index.
   * @param logicalAxis {@link FlixelGamepadInput#AXIS_LEFT_X} or similar.
   */
  @NotNull
  public static FlixelAnalogAxisBinding gamepadAxisX(int gamepadSlot, int logicalAxis) {
    return new FlixelAnalogAxisBinding(Kind.GAMEPAD_AXIS_X, logicalAxis, gamepadSlot);
  }

  /**
   * Adds the Y component of a stick with the screen-space inversion corrected so that up = positive Y,
   * matching the keyboard key convention. This is the correct choice for almost every game.
   *
   * @param gamepadSlot {@code 0..} slot index.
   * @param logicalAxis {@link FlixelGamepadInput#AXIS_LEFT_Y} or similar.
   */
  @NotNull
  public static FlixelAnalogAxisBinding gamepadAxisY(int gamepadSlot, int logicalAxis) {
    return gamepadAxisY(gamepadSlot, logicalAxis, false);
  }

  /**
   * Adds the Y component of a stick.
   *
   * @param gamepadSlot {@code 0..} slot index.
   * @param logicalAxis {@link FlixelGamepadInput#AXIS_LEFT_Y} or similar.
   * @param raw When {@code false} (the default), the raw hardware Y is negated so that up = positive Y,
   *   matching keyboard key bindings. Pass {@code true} to use the raw screen-space value unchanged.
   */
  @NotNull
  public static FlixelAnalogAxisBinding gamepadAxisY(int gamepadSlot, int logicalAxis, boolean raw) {
    return new FlixelAnalogAxisBinding(raw ? Kind.RAW_GAMEPAD_AXIS_Y : Kind.GAMEPAD_AXIS_Y, logicalAxis, gamepadSlot);
  }

  /** Binding kinds used by {@link FlixelActionAnalog} to accumulate the vector each frame. */
  public enum Kind {
    KEY_NEG_X,
    KEY_POS_X,
    KEY_NEG_Y,
    KEY_POS_Y,
    GAMEPAD_AXIS_X,
    /** Y axis with screen-space inversion corrected (up = positive Y). Default from {@link #gamepadAxisY(int, int)}. */
    GAMEPAD_AXIS_Y,
    /** Raw hardware Y axis without any correction. */
    RAW_GAMEPAD_AXIS_Y
  }
}
