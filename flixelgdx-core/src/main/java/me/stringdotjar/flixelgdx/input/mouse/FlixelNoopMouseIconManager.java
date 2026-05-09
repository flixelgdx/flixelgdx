/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.mouse;

import org.jetbrains.annotations.NotNull;

/**
 * {@link FlixelMouseIconManager} implementation used on backends without native cursor control.
 */
public enum FlixelNoopMouseIconManager implements FlixelMouseIconManager {

  /** Shared instance. */
  INSTANCE;

  @Override
  public void setNativeCursor(@NotNull FlixelNativeMouseCursor cursor) {}

  @Override
  public void clearNativeCursor() {}

  @Override
  public boolean supportsNativeCursor() {
    return false;
  }
}
