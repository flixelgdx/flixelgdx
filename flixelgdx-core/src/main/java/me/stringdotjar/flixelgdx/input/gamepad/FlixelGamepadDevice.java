/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

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

  public FlixelGamepadDevice(@NotNull FlixelGamepadManager manager, int id) {
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
