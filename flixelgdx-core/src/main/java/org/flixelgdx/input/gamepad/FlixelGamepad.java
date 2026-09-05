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
 * {@link FlixelGamepadInputManager} through a {@link FlixelGamepadProvider}. The manager reads raw
 * button and axis state through here every frame; game code stays one level up, at
 * {@code Flixel.gamepads}, and never touches this interface directly.
 *
 * <p>Button and axis indices here are the controller's own <b>native</b> indices. Translate between
 * native and logical inputs with the {@link FlixelGamepadMapping} the manager resolves at connect
 * time via its {@link FlixelGamepadMappingResolver} chain.
 *
 * <p>Vendor and product IDs are the preferred way for resolvers to identify a gamepad. They are
 * stable across OS versions and driver updates. When a backend cannot expose them, both return
 * {@code 0} and resolvers should fall back to {@link #getName()}.
 *
 * @see FlixelGamepadProvider
 * @see FlixelGamepadMapping
 * @see FlixelGamepadMappingResolver
 */
public interface FlixelGamepad {

  /**
   * Returns a human-readable device name (for example {@code "Xbox Wireless Controller"}); never {@code null}.
   *
   * <p>Useful as a fallback in resolvers when VID/PID are unavailable, but avoid matching on names
   * alone - they vary across drivers and OS versions.
   *
   * @return The gamepad's display name, never {@code null}.
   */
  @NotNull
  String getName();

  /**
   * Returns the USB vendor ID for this gamepad, or {@code 0} when unavailable.
   *
   * <p>Together with {@link #getProductId()}, this is the most reliable way to identify a specific
   * controller model. Resolvers should prefer VID/PID checks over name matching.
   *
   * @return USB vendor ID in the range {@code [0, 0xFFFF]}, or {@code 0} when unknown.
   */
  default int getVendorId() {
    return 0;
  }

  /**
   * Returns the USB product ID for this gamepad, or {@code 0} when unavailable.
   *
   * @return USB product ID in the range {@code [0, 0xFFFF]}, or {@code 0} when unknown.
   */
  default int getProductId() {
    return 0;
  }

  /**
   * Returns the lowest native button index this gamepad can report.
   *
   * <p>Together with {@link #getMaxButtonIndex()} it bounds the range to scan when polling buttons.
   *
   * @return The minimum native button index supported by this gamepad.
   */
  int getMinButtonIndex();

  /**
   * Returns the highest native button index this gamepad can report.
   *
   * @return The maximum native button index supported by this gamepad.
   */
  int getMaxButtonIndex();

  /**
   * Returns {@code true} while the given button is held down.
   *
   * @param buttonIndex A native button index.
   * @return {@code true} if the button is currently pressed, {@code false} otherwise.
   */
  boolean getButton(int buttonIndex);

  /**
   * Returns how many analog axes this gamepad exposes.
   *
   * @return The number of analog axes available on this gamepad.
   */
  int getAxisCount();

  /**
   * Returns the axis value, normally in the range {@code [-1, 1]}.
   *
   * @param axisIndex A native axis index.
   * @return The current value of the axis, typically in the range {@code [-1, 1]}.
   */
  float getAxis(int axisIndex);

  /**
   * Returns {@code true} when this gamepad reports that it can vibrate.
   *
   * @return {@code true} if vibration is supported, {@code false} otherwise.
   */
  boolean canVibrate();

  /**
   * Starts a rumble at the given unified strength for the given time. Backends with independent
   * motors are driven through {@link FlixelGamepadHapticsProvider} instead; this is the simple fallback.
   *
   * @param durationMs How long to vibrate, in milliseconds.
   * @param strength Motor strength in the range {@code [0, 1]}.
   */
  void startVibration(int durationMs, float strength);

  /** Stops any active vibration on this gamepad immediately. */
  void cancelVibration();

  /**
   * Returns the backend's underlying native controller object, or {@code null} when there is none.
   *
   * <p>This is a deliberate, explicitly-unsafe escape hatch for advanced platform-specific features
   * (for example reaching a backend's raw rumble API or resolving VID/PID when the backend does not
   * expose them directly). The returned type depends entirely on the active backend and is not part
   * of the stable API, so casting it ties your code to that backend. Ordinary games never need this.
   *
   * @return The native controller handle, or {@code null} when unavailable.
   */
  @Nullable
  default Object getNativeHandle() {
    return null;
  }
}
