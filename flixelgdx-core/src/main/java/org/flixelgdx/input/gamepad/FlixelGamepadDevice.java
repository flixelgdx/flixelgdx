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
 * Optional per-slot facade for gamepad queries. Instances are created only when the game calls
 * {@link FlixelGamepadManager#ensureDevice(int)}; {@link FlixelGamepadManager#getById(int)} stays
 * {@code null} until then.
 */
public final class FlixelGamepadDevice {

  private final FlixelGamepadManager manager;
  private final int id;

  FlixelGamepadDevice(@NotNull FlixelGamepadManager manager, int id) {
    this.manager = manager;
    this.id = id;
  }

  /**
   * Slot index for this device (stable while the controller stays at the same list index in the
   * backend).
   *
   * @return Gamepad id between {@code 0} and {@link FlixelGamepadManager#MAX_GAMEPADS} exclusive.
   */
  public int getId() {
    return id;
  }

  /**
   * Whether this slot currently maps to a connected controller.
   *
   * @return {@code true} when connected.
   */
  public boolean isConnected() {
    return manager.isSlotConnected(id);
  }

  /** Returns whether this gamepad slot currently maps to a connected controller. */
  public boolean getConnected() {
    return manager.isSlotConnected(id);
  }

  /**
   * Model detected for this slot the last time the slot was (re)bound.
   *
   * @return Detected model, or {@link FlixelGamepadModel#UNKNOWN} when out of range.
   */
  @NotNull
  public FlixelGamepadModel getModel() {
    return manager.getModel(id);
  }

  /**
   * Returns {@code true} when this controller is currently pressing the given button.
   *
   * @param logicalButton A logical button constant from {@link FlixelGamepadInput}.
   * @return {@code true} when the button is held this frame.
   */
  public boolean pressed(int logicalButton) {
    return manager.pressed(id, logicalButton);
  }

  /**
   * Returns {@code true} when this controller first pressed the button this frame.
   *
   * @param logicalButton A logical button constant from {@link FlixelGamepadInput}.
   * @return {@code true} on the first frame the button is pressed.
   */
  public boolean justPressed(int logicalButton) {
    return manager.justPressed(id, logicalButton);
  }

  /**
   * Returns {@code true} when this controller released the button this frame.
   *
   * @param logicalButton A logical button constant from {@link FlixelGamepadInput}.
   * @return {@code true} on the first frame the button is no longer pressed.
   */
  public boolean justReleased(int logicalButton) {
    return manager.justReleased(id, logicalButton);
  }

  /**
   * Returns whether this controller reports vibration support.
   *
   * @return {@code true} when connected and the hardware supports vibration.
   */
  public boolean canVibrate() {
    return manager.canVibrate(id);
  }

  /**
   * Vibrates this controller at full intensity on both motors for the given duration.
   *
   * @param durationSecs How long to vibrate in seconds.
   */
  public void vibrate(float durationSecs) {
    manager.vibrate(id, durationSecs);
  }

  /**
   * Vibrates this controller at the given intensity on both motors.
   *
   * @param intensity Motor strength in the range {@code [0, 1]}.
   * @param durationSecs How long to vibrate in seconds.
   */
  public void vibrate(float intensity, float durationSecs) {
    manager.vibrate(id, intensity, durationSecs);
  }

  /**
   * Vibrates this controller with independent left and right motor intensities.
   *
   * @param leftIntensity Strength for the left (low-frequency) motor, in the range {@code [0, 1]}.
   * @param rightIntensity Strength for the right (high-frequency) motor, in the range {@code [0, 1]}.
   * @param durationSecs How long to vibrate in seconds.
   */
  public void vibrate(float leftIntensity, float rightIntensity, float durationSecs) {
    manager.vibrate(id, leftIntensity, rightIntensity, durationSecs);
  }

  /**
   * Stops any active vibration on this controller immediately.
   */
  public void stopVibration() {
    manager.stopVibration(id);
  }

  /**
   * Returns the current value of a logical axis on this controller, after dead-zone processing.
   *
   * @param logicalAxis A logical axis constant from {@link FlixelGamepadInput}.
   * @return Axis value in the range {@code [-1, 1]}, or {@code 0f} when inactive or within the
   *     dead zone.
   */
  public float getAxis(int logicalAxis) {
    return manager.getAxis(id, logicalAxis);
  }

  /**
   * Shorthand for the left stick horizontal axis ({@link FlixelGamepadInput#AXIS_LEFT_X}).
   *
   * @return Horizontal axis value in the range {@code [-1, 1]}.
   */
  public float getXAxis() {
    return manager.getAxis(id, FlixelGamepadInput.AXIS_LEFT_X);
  }

  /**
   * Shorthand for the left stick vertical axis ({@link FlixelGamepadInput#AXIS_LEFT_Y}).
   *
   * @return Vertical axis value in the range {@code [-1, 1]}.
   */
  public float getYAxis() {
    return manager.getAxis(id, FlixelGamepadInput.AXIS_LEFT_Y);
  }

  /**
   * Analog pressure of the left trigger (L2), in the range {@code [0, 1]}, after dead-zone
   * processing.
   *
   * <p>On Jamepad/SDL desktop, triggers are axes; this reads the trigger axis directly.
   * On web (TeaVM/W3C Gamepad API), triggers are digital buttons and this always returns {@code 0}
   * - use {@link #pressed(int)} with {@link FlixelGamepadInput#L2} there instead.
   *
   * @return Trigger pressure in {@code [0, 1]}, or {@code 0f} within the dead zone.
   */
  public float getTriggerL() {
    return manager.getTriggerL(id);
  }

  /**
   * Analog pressure of the right trigger (R2), in the range {@code [0, 1]}, after dead-zone
   * processing.
   *
   * <p>On Jamepad/SDL desktop, triggers are axes; this reads the trigger axis directly.
   * On web (TeaVM/W3C Gamepad API), triggers are digital buttons and this always returns {@code 0}
   * - use {@link #pressed(int)} with {@link FlixelGamepadInput#R2} there instead.
   *
   * @return Trigger pressure in {@code [0, 1]}, or {@code 0f} within the dead zone.
   */
  public float getTriggerR() {
    return manager.getTriggerR(id);
  }
}
