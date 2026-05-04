/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input;

/**
 * Shared per-frame contract for polled input managers (keyboard, mouse, gamepads).
 *
 * <p>Call {@link #update()} once near the start of the frame, then {@link #endFrame()} after game
 * logic and rendering so edge-triggered helpers (for example {@code justPressed}) stay valid for
 * the full frame, matching {@link me.stringdotjar.flixelgdx.FlixelGame}.
 */
public interface FlixelInputManager {

  /** Reads hardware state and refreshes internal snapshots for this frame. */
  void update();

  /**
   * Finalizes this frame after gameplay and draw hooks run. Typically, it copies the current snapshot
   * into the previous snapshot used on the next frame for edge detection.
   */
  void endFrame();

  /** Clears internal state. Default implementation does nothing. */
  default void reset() {}
}
