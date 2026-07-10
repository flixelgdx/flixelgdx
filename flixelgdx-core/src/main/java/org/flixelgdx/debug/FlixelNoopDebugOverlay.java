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
 * A fully inert debug overlay used as the default value of
 * {@link org.flixelgdx.debug.FlixelDebugManager#overlay FlixelDebugManager.overlay} so callers
 * never need to null-check the field.
 *
 * <p>All lifecycle methods ({@link #update(float)}, {@link #resize(int, int)}, {@link #drawUI()})
 * are no-ops. The singleton is safe to share across the application's lifetime.
 */
public final class FlixelNoopDebugOverlay extends FlixelDebugOverlay {

  /** Shared singleton instance used as the default overlay before debug mode starts. */
  public static final FlixelNoopDebugOverlay INSTANCE = new FlixelNoopDebugOverlay();

  private FlixelNoopDebugOverlay() {}

  @Override
  public void update(float elapsed) {}

  @Override
  public void resize(int width, int height) {}

  @Override
  protected void drawUI() {}

  @Override
  public void destroy() {}
}
