/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.functional;

/**
 * Something that can be shown or hidden for drawing. Matches the usual {@code visible} flag on
 * {@link me.stringdotjar.flixelgdx.FlixelBasic}.
 */
public interface FlixelVisible {

  boolean isVisible();

  void setVisible(boolean visible);

  /** Flips between visible and hidden. */
  void toggleVisible();
}
