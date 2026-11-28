/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.input;

import org.flixelgdx.FlixelManagers;

/**
 * Shared per-frame contract for polled input managers (keyboard, mouse, touches, gamepads).
 *
 * <p>Every input manager is a {@link FlixelManagers.Manager} that keeps updating while the game is
 * paused. Its {@link #update(float)} hook reads the hardware through {@link #update()} near the
 * start of the frame, and its {@link #endFrame()} hook runs after game logic and rendering, so
 * edge-triggered helpers (for example {@code justPressed}) stay valid for the full frame.
 */
public interface FlixelInputManager extends FlixelManagers.Manager {

  /** Reads hardware state and refreshes internal snapshots for this frame. */
  void update();

  /**
   * Reads the hardware for this frame by calling {@link #update()}.
   *
   * @param elapsed The scaled seconds since the last frame. Input does not use it.
   */
  @Override
  default void update(float elapsed) {
    update();
  }

  /**
   * Returns {@code false}, since input must keep updating while the game is paused (for example so
   * a pause menu or the debugger can still read it).
   *
   * @return Always {@code false}.
   */
  @Override
  default boolean isPausable() {
    return false;
  }

  /**
   * Finalizes this frame after gameplay and draw hooks run. Typically, it copies the current snapshot
   * into the previous snapshot used on the next frame for edge detection.
   */
  @Override
  void endFrame();

  /** Clears internal state. Default implementation does nothing. */
  default void reset() {}
}
