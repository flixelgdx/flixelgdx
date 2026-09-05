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
import org.jetbrains.annotations.Nullable;

/**
 * The platform's supply of connected gamepads: how {@link FlixelGamepadInputManager} discovers
 * pads and hears about them connecting or disconnecting, without naming a controller library.
 *
 * <p>This is only the raw feed. It deliberately holds <b>no</b> gamepad logic: slot assignment,
 * button and axis polling, dead zones, press-order tracking, and event dispatch all live in the
 * manager. Each backend implements this to expose its own controllers, and the manager stays the
 * single owner of behavior. It works exactly like {@link FlixelGamepadHapticsProvider}: a thin, swappable
 * platform binding the manager drives.
 *
 * <p>The manager enumerates gamepads every frame through {@link #getGamepadCount()} and
 * {@link #getGamepadAt(int)}, so implementations must return the <b>same</b>
 * {@link FlixelGamepad} instance for the same physical device across calls (the manager tracks
 * slots by identity), and must not allocate on these calls.
 *
 * <p>A safe default ({@link FlixelNoopGamepadProvider}) reports no gamepads, so the manager
 * runs cleanly on headless sessions and platforms without a gamepad backend yet.
 *
 * @see FlixelGamepadInputManager#setGamepadProvider(FlixelGamepadProvider)
 */
public interface FlixelGamepadProvider {

  /**
   * Returns how many gamepads are connected right now. Defaults to {@code 0}.
   *
   * @return The number of currently connected gamepads.
   */
  default int getGamepadCount() {
    return 0;
  }

  /**
   * Returns the connected gamepad at the given position.
   *
   * @param index A position from {@code 0} to {@link #getGamepadCount()} minus one.
   * @return The gamepad at that position, or {@code null} when the index is out of range. The
   *     same physical device must always map to the same instance.
   */
  @Nullable
  default FlixelGamepad getGamepadAt(int index) {
    return null;
  }

  /**
   * Starts delivering connect/disconnect (and button/axis) events to the given listener.
   *
   * @param listener The listener to notify; ignored when {@code null}.
   */
  default void addListener(@NotNull FlixelGamepadListener listener) {}

  /**
   * Stops delivering events to the given listener.
   *
   * @param listener The listener to remove; ignored when {@code null}.
   */
  default void removeListener(@NotNull FlixelGamepadListener listener) {}
}
