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

import org.jetbrains.annotations.NotNull;

/**
 * Notified when gamepads connect or disconnect, and (optionally) on individual button and axis
 * events.
 *
 * <p>{@link FlixelGamepadInputManager} implements this and registers itself with the active
 * {@link FlixelGamepadProvider} so it can keep its slots in sync as pads come and go. Every method
 * has a do-nothing default, so an implementation only overrides what it cares about.
 *
 * @see FlixelGamepadProvider#addListener(FlixelGamepadListener)
 */
public interface FlixelGamepadListener {

  /**
   * Called when a gamepad is connected.
   *
   * @param gamepad The gamepad that connected.
   */
  default void connected(@NotNull FlixelGamepad gamepad) {}

  /**
   * Called when a gamepad is disconnected.
   *
   * @param gamepad The gamepad that disconnected.
   */
  default void disconnected(@NotNull FlixelGamepad gamepad) {}

  /**
   * Called when a button is pressed on a gamepad.
   *
   * @param gamepad The gamepad the event came from.
   * @param buttonIndex The native button index that went down.
   * @return {@code true} to consume the event.
   */
  default boolean buttonDown(@NotNull FlixelGamepad gamepad, int buttonIndex) {
    return false;
  }

  /**
   * Called when a button is released on a gamepad.
   *
   * @param gamepad The gamepad the event came from.
   * @param buttonIndex The native button index that came up.
   * @return {@code true} to consume the event.
   */
  default boolean buttonUp(@NotNull FlixelGamepad gamepad, int buttonIndex) {
    return false;
  }

  /**
   * Called when an axis moves on a gamepad.
   *
   * @param gamepad The gamepad the event came from.
   * @param axisIndex The native axis index that moved.
   * @param value The new axis value, normally in the range {@code [-1, 1]}.
   * @return {@code true} to consume the event.
   */
  default boolean axisMoved(@NotNull FlixelGamepad gamepad, int axisIndex, float value) {
    return false;
  }
}
