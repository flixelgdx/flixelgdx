/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.action;

/**
 * Discriminator for {@link FlixelInputBinding}. Keeping keyboard, gamepad, pointer, and touch region in separate enum
 * values lets {@link FlixelActionDigital} store {@code int} fields without overloading the same number for different
 * devices (for example a key code versus a gamepad logical button).
 */
public enum FlixelInputBindingKind {

  /** {@link me.stringdotjar.flixelgdx.input.keyboard.FlixelKey} / {@link com.badlogic.gdx.Input.Keys} keycode. */
  KEY,

  /**
   * Logical gamepad button from {@link me.stringdotjar.flixelgdx.input.gamepad.FlixelGamepadInput}
   * plus a slot index, or {@code -1} for any connected slot (uses {@code Flixel.gamepads.anyPressed} style queries).
   */
  GAMEPAD_BUTTON,

  /**
   * Mouse button when pointer is {@code -1}, otherwise {@link com.badlogic.gdx.Gdx#input} multitouch
   * {@code isTouched(pointer)} with optional button filter.
   */
  POINTER_BUTTON,

  /** Normalized screen rectangle {@code 0..1} for touch-like region hits. */
  TOUCH_REGION
}
