/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.teavm.debug;

import me.stringdotjar.flixelgdx.debug.FlixelDebugOverlay;

/**
 * Web (TeaVM) debug overlay: reuses the shared {@link FlixelDebugOverlay} controller (pause,
 * camera tools, hitboxes, watch/log buffers) without an extra UI toolkit. Replace
 * {@link me.stringdotjar.flixelgdx.Flixel#setDebugOverlay(java.util.function.Supplier)} if you add a
 * browser-based panel layer later.
 */
public final class FlixelTeaVMDebugOverlay extends FlixelDebugOverlay {

  @Override
  protected void drawUI() {
    // No Dear ImGui on web yet; stats and commands remain available through Flixel.log / console.
  }
}
