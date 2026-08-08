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
 * A single connected gamepad, as seen by the framework without naming any specific controller
 * library.
 *
 * <p>Each platform backend implements this over its own controller objects and hands them to
 * {@link FlixelGamepadInputManager} through a {@link FlixelControllerProvider}. The manager reads raw
 * button and axis state through here every frame and resolves it against {@link #getMapping()}; game
 * code stays one level up, at {@code Flixel.gamepads}, and never touches this interface directly.
 *
 * <p>Button and axis indices here are the controller's own <b>native</b> indices, not the logical
 * {@link FlixelGamepadInput} constants. Translate between the two with {@link #getMapping()}.
 *
 * @see FlixelControllerProvider
 * @see FlixelControllerMapping
 */
public interface FlixelController {

  /**
   * @return A human-readable device name (for example {@code "Xbox Wireless Controller"}), used for
   *     model detection; never {@code null}.
   */
  @NotNull
  String getName();

  /**
   * @return This controller's translation table from logical inputs to native indices; never
   *     {@code null}.
   */
  @NotNull
  FlixelControllerMapping getMapping();

  /**
   * @return The lowest native button index this controller can report. Together with
   *     {@link #getMaxButtonIndex()} it bounds the range to scan when polling buttons.
   */
  int getMinButtonIndex();

  /**
   * @return The highest native button index this controller can report.
   */
  int getMaxButtonIndex();

  /**
   * @param buttonIndex A native button index (not a {@link FlixelGamepadInput} constant).
   * @return {@code true} while that button is held down.
   */
  boolean getButton(int buttonIndex);

  /**
   * @return How many analog axes this controller exposes.
   */
  int getAxisCount();

  /**
   * @param axisIndex A native axis index (not a {@link FlixelGamepadInput} constant).
   * @return The axis value, normally in the range {@code [-1, 1]}.
   */
  float getAxis(int axisIndex);

  /**
   * @return {@code true} when this controller reports that it can vibrate.
   */
  boolean canVibrate();

  /**
   * Starts a rumble at the given unified strength for the given time. Backends with independent
   * motors are driven through {@link FlixelHapticsProvider} instead; this is the simple fallback.
   *
   * @param durationMs How long to vibrate, in milliseconds.
   * @param strength Motor strength in the range {@code [0, 1]}.
   */
  void startVibration(int durationMs, float strength);

  /** Stops any active vibration on this controller immediately. */
  void cancelVibration();

  /**
   * Returns the backend's underlying native controller object, or {@code null} when there is none.
   *
   * <p>This is a deliberate, explicitly-unsafe escape hatch for advanced platform-specific features
   * (for example reaching a backend's raw rumble API). The returned type depends entirely on the
   * active backend and is not part of the stable API, so casting it ties your code to that backend.
   * Ordinary games never need this.
   *
   * @return The native controller handle, or {@code null} when unavailable.
   */
  @Nullable
  default Object getNativeHandle() {
    return null;
  }
}
