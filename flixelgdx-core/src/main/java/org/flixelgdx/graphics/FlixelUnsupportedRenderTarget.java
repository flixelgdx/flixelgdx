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
package org.flixelgdx.graphics;

import org.jetbrains.annotations.NotNull;

/**
 * No-op {@link FlixelRenderTarget} returned by
 * {@link FlixelGraphicsManager#createRenderTarget(int, int)} when no graphics backend is present.
 *
 * <p>Every operation does nothing and the texture is a zero-sized stand-in, so post-processing
 * code paths degrade gracefully on headless sessions.
 */
public enum FlixelUnsupportedRenderTarget implements FlixelRenderTarget {

  /** Shared no-op instance. */
  INSTANCE;

  private final FlixelTexture texture = new FlixelNoopTexture(1, 1);

  @Override
  public void begin() {}

  @Override
  public void end() {}

  @Override
  public void destroy() {}

  @Override
  public int getWidth() {
    return 0;
  }

  @Override
  public int getHeight() {
    return 0;
  }

  @NotNull
  @Override
  public FlixelTexture getTexture() {
    return texture;
  }

  @Override
  public boolean isFlipped() {
    return false;
  }
}
