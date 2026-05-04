/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.action;

import me.stringdotjar.flixelgdx.input.gamepad.FlixelGamepadInput;

import org.jetbrains.annotations.NotNull;

/**
 * One contributor to a {@link FlixelActionAnalog} vector. Combine several bindings: WASD-style
 * {@link Kind#KEY_NEG_X}, {@link Kind#KEY_POS_X}, and stick channels {@link Kind#GAMEPAD_AXIS_X} /
 * {@link Kind#GAMEPAD_AXIS_Y} for the same physical stick on a slot.
 *
 * <p>Create bindings only during setup. Gamepad axes use {@link me.stringdotjar.flixelgdx.input.gamepad.FlixelGamepadInput}
 * logical constants; {@link me.stringdotjar.flixelgdx.Flixel#gamepads} applies the same dead zone as the rest of a game.
 */
public final class FlixelAnalogAxisBinding {

  public enum Kind {
    KEY_NEG_X,
    KEY_POS_X,
    KEY_NEG_Y,
    KEY_POS_Y,
    GAMEPAD_AXIS_X,
    GAMEPAD_AXIS_Y
  }

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
   * Adds the X component of a stick (or single axis read as X only).
   *
   * @param gamepadSlot {@code 0..} slot index.
   * @param logicalAxis {@link FlixelGamepadInput#AXIS_LEFT_X} or similar.
   */
  @NotNull
  public static FlixelAnalogAxisBinding gamepadAxisX(int gamepadSlot, int logicalAxis) {
    return new FlixelAnalogAxisBinding(Kind.GAMEPAD_AXIS_X, logicalAxis, gamepadSlot);
  }

  /**
   * Adds the Y component of a stick.
   *
   * @param gamepadSlot {@code 0..} slot index.
   * @param logicalAxis {@link FlixelGamepadInput#AXIS_LEFT_Y} or similar.
   */
  @NotNull
  public static FlixelAnalogAxisBinding gamepadAxisY(int gamepadSlot, int logicalAxis) {
    return new FlixelAnalogAxisBinding(Kind.GAMEPAD_AXIS_Y, logicalAxis, gamepadSlot);
  }
}
