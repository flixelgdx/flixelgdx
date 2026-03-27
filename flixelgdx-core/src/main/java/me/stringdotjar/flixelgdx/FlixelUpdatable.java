/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx;

/**
 * Interface for Flixel-based objects that can be updated.
 *
 * @see FlixelBasic
 */
public interface FlixelUpdatable {

  /**
   * Updates the object for the given elapsed time.
   *
   * @param elapsed The elapsed time since the last frame update.
   */
  void update(float elapsed);
}
