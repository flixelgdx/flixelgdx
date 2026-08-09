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

/**
 * A texture stand-in used when no graphics backend is present (headless sessions, unit tests,
 * and pre-startup code paths).
 *
 * <p>It remembers its size and filter flag so layout and logic code behaves normally, but owns
 * no GPU memory and ignores pixel updates. {@link FlixelNoopGraphicsManager} returns these from
 * every texture-creating call so nothing crashes without a GPU.
 */
public class FlixelNoopTexture implements FlixelTexture {

  private final int width;
  private final int height;

  private boolean smooth;

  /**
   * Creates a no-op texture that reports the given size.
   *
   * @param width Reported width in pixels.
   * @param height Reported height in pixels.
   */
  public FlixelNoopTexture(int width, int height) {
    this.width = width;
    this.height = height;
  }

  @Override
  public void update(int x, int y, FlixelImage image) {}

  @Override
  public void destroy() {}

  @Override
  public long getHandle() {
    return 0L;
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  public boolean isSmooth() {
    return smooth;
  }

  @Override
  public void setSmooth(boolean smooth) {
    this.smooth = smooth;
  }
}
