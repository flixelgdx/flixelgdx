/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.action;

import org.jetbrains.annotations.NotNull;

/**
 * Built-in {@link FlixelSteamActionReader} instances. Use {@link #EMPTY} on {@link FlixelActionSet#steamReader} when Steam
 * is not linked so field reads stay null-safe without custom classes.
 */
public final class FlixelSteamActionReaders {

  /** Reader that always reports inactive digital input and zero analog vectors. */
  @NotNull
  public static final FlixelSteamActionReader EMPTY = new FlixelSteamActionReader() {
    @Override
    public boolean getDigital(@NotNull String actionName) {
      return false;
    }

    @Override
    public float getAnalogX(@NotNull String actionName) {
      return 0f;
    }

    @Override
    public float getAnalogY(@NotNull String actionName) {
      return 0f;
    }
  };

  private FlixelSteamActionReaders() {}
}
