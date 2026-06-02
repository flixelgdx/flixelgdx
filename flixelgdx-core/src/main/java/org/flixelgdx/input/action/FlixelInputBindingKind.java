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

/**
 * Discriminator for {@link FlixelInputBinding}. Keeping keyboard, gamepad, pointer, and touch region in separate enum
 * values lets {@link FlixelActionDigital} store {@code int} fields without overloading the same number for different
 * devices (for example a key code versus a gamepad logical button).
 */
public enum FlixelInputBindingKind {

  /** {@link org.flixelgdx.input.keyboard.FlixelKey} / {@link com.badlogic.gdx.Input.Keys} keycode. */
  KEY,

  /**
   * Logical gamepad button from {@link org.flixelgdx.input.gamepad.FlixelGamepadInput}
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
