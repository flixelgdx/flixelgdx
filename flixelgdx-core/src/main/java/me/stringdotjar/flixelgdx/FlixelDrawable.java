/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx;

import com.badlogic.gdx.graphics.g2d.Batch;

/**
 * Interface for Flixel-based visible objects that can be drawn.
 *
 * @see FlixelBasic
 */
public interface FlixelDrawable {

  /**
   * Draws the visible object.
   *
   * @param batch The batch used for rendering.
   */
  void draw(Batch batch);
}
