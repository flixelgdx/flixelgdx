/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.debug;

/**
 * Debug overlay with no extra UI: the full {@link FlixelDebugOverlay} controller (stats, watch,
 * log buffers, pause/camera tools, sprite picking) runs, but {@link #drawUI()} is a no-op.
 *
 * <p>Use this as the default {@link me.stringdotjar.flixelgdx.Flixel#setDebugOverlay(java.util.function.Supplier)}
 * when a platform does not ship a richer debugger yet (for example headless tests or backends
 * without Dear ImGui). Desktop launchers typically replace it with a platform-specific subclass.
 */
public final class FlixelHeadlessDebugOverlay extends FlixelDebugOverlay {

  @Override
  protected void drawUI() {
    // Intentionally empty: no panels; hitboxes and pause tools still work from the base class.
  }
}
