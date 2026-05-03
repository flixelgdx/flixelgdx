/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.gamepad;

import com.badlogic.gdx.controllers.Controller;

import org.jetbrains.annotations.NotNull;

/**
 * Wii family mapping. Currently uses the same translation as {@link FlixelStandardGamepadMapping}
 * while still reporting {@link FlixelGamepadModel#WII}; override button swaps here if a concrete
 * Wii layout is required for your targets.
 */
public final class FlixelWiiGamepadMapping extends FlixelGamepadMapping {

  /** Shared instance for {@link FlixelGamepadModel#WII}. */
  public static final FlixelWiiGamepadMapping INSTANCE = new FlixelWiiGamepadMapping();

  private final FlixelStandardGamepadMapping delegate = new FlixelStandardGamepadMapping(FlixelGamepadModel.WII);

  private FlixelWiiGamepadMapping() {}

  @Override
  @NotNull
  public FlixelGamepadModel getModel() {
    return FlixelGamepadModel.WII;
  }

  @Override
  public int toNativeButton(@NotNull Controller controller, int logicalButton) {
    return delegate.toNativeButton(controller, logicalButton);
  }

  @Override
  public int toNativeAxis(@NotNull Controller controller, int logicalAxis) {
    return delegate.toNativeAxis(controller, logicalAxis);
  }
}
