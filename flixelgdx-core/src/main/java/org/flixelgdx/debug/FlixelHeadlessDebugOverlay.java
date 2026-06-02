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
package org.flixelgdx.debug;

/**
 * Debug overlay with no extra UI: the full {@link FlixelDebugOverlay} controller (stats, watch,
 * log buffers, pause/camera tools, sprite picking) runs, but {@link #drawUI()} is a no-op.
 *
 * <p>Use this as the default {@link org.flixelgdx.Flixel#setDebugOverlay(java.util.function.Supplier)}
 * when a platform does not ship a richer debugger yet (for example headless tests or backends
 * without Dear ImGui). Desktop launchers typically replace it with a platform-specific subclass.
 */
public final class FlixelHeadlessDebugOverlay extends FlixelDebugOverlay {

  @Override
  protected void drawUI() {
    // Intentionally empty: no panels; hitboxes and pause tools still work from the base class.
  }
}
