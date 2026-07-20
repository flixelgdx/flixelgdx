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

/**
 * Pluggable vibration backend for {@link FlixelGamepadInputManager}.
 *
 * <p>The built-in implementation, {@link FlixelDefaultHapticsProvider}, delegates to
 * gdx-controllers' {@link com.badlogic.gdx.controllers.Controller#startVibration} and works on
 * desktop (SDL via {@code gdx-controllers-desktop}) and web (W3C Gamepad API via
 * {@code gdx-controllers-teavm}) without any extra setup. For platform-specific features such as
 * dual-motor channels, haptic patterns, or DualSense adaptive triggers, supply a custom
 * implementation via {@link FlixelGamepadInputManager#setHapticsProvider}.
 *
 * <h2>Intensity values</h2>
 *
 * <p>All intensity parameters are normalized to the range {@code [0, 1]}, where {@code 0f} means
 * no vibration and {@code 1f} means full motor strength. Implementations should clamp values
 * outside this range rather than letting them reach the hardware unchecked.
 *
 * <h2>Slot indices</h2>
 *
 * <p>Each method receives the same slot id used throughout {@link FlixelGamepadInputManager}. The
 * manager already validates that the slot is in range and that the gamepad system is enabled
 * before calling the provider, so implementations do not need to repeat those checks.
 */
public interface FlixelHapticsProvider {

  /**
   * Vibrates the controller in the given slot.
   *
   * <p>The built-in providers for desktop and web honor both motor channels independently.
   * Implementations targeting single-motor hardware may collapse the two intensities to a single
   * value, typically the larger of the two.
   *
   * @param slot Slot index between {@code 0} and {@link FlixelGamepadInputManager#MAX_GAMEPADS} exclusive.
   * @param leftIntensity Strength for the left (low-frequency) motor, in the range {@code [0, 1]}.
   * @param rightIntensity Strength for the right (high-frequency) motor, in the range {@code [0, 1]}.
   * @param durationSecs How long to vibrate, in seconds.
   */
  void vibrate(int slot, float leftIntensity, float rightIntensity, float durationSecs);

  /**
   * Stops any active vibration on the controller in the given slot immediately.
   *
   * @param slot Slot index.
   */
  void stopVibration(int slot);

  /**
   * Returns whether the controller in the given slot reports haptics support.
   *
   * @param slot Slot index.
   * @return {@code true} when the slot is connected and the hardware supports vibration.
   */
  boolean canVibrate(int slot);
}
