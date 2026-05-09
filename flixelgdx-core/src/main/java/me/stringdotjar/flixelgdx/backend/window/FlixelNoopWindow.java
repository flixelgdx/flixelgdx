/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.window;

/**
 * Safe default {@link FlixelWindow} for mobile, web, and headless targets.
 */
public enum FlixelNoopWindow implements FlixelWindow {

  /** Shared no-op instance. */
  INSTANCE;

  @Override
  public void setWindowOpacity(float opacity) {}

  @Override
  public boolean supportsWindowOpacity() {
    return false;
  }
}
