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

/**
 * Shared per-frame contract for polled input managers (keyboard, mouse, gamepads).
 *
 * <p>Call {@link #update()} once near the start of the frame, then {@link #endFrame()} after game
 * logic and rendering so edge-triggered helpers (for example {@code justPressed}) stay valid for
 * the full frame, matching {@link org.flixelgdx.FlixelGame}.
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
  default void reset() {
  }
}
