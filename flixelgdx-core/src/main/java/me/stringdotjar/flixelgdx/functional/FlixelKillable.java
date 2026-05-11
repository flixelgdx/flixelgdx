/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.functional;

/**
 * Flixel-style kill and revive: a killed object should not run normal updates or draws, but can be
 * revived later without reallocating. See {@link me.stringdotjar.flixelgdx.FlixelBasic#kill()} and
 * {@link me.stringdotjar.flixelgdx.FlixelBasic#revive()}.
 */
public interface FlixelKillable {

  /**
   * @return {@code true} when this instance is killed (disabled) in the Flixel sense.
   */
  boolean isKilled();

  /**
   * @param killed {@code true} to {@link #kill()}, {@code false} to {@link #revive()}.
   */
  void setKilled(boolean killed);

  void toggleKilled();

  void kill();

  void revive();
}
