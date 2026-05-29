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
package me.stringdotjar.flixelgdx.input.gamepad;

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

  /**
   * Model detected for this slot the last time the slot was (re)bound.
   *
   * @return Detected model, or {@link FlixelGamepadModel#UNKNOWN} when out of range.
   */
  @NotNull
  public FlixelGamepadModel getModel() {
    return manager.getDetectedModel(id);
  }

  public boolean pressed(int logicalButton) {
    return manager.pressed(id, logicalButton);
  }

  public boolean justPressed(int logicalButton) {
    return manager.justPressed(id, logicalButton);
  }

  public boolean justReleased(int logicalButton) {
    return manager.justReleased(id, logicalButton);
  }

  public float getAxis(int logicalAxis) {
    return manager.getAxis(id, logicalAxis);
  }

  public float getXAxis() {
    return manager.getAxis(id, FlixelGamepadInput.AXIS_LEFT_X);
  }

  public float getYAxis() {
    return manager.getAxis(id, FlixelGamepadInput.AXIS_LEFT_Y);
  }
}
