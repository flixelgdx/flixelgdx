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

import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * An image that lives in normal memory (not on the GPU), stored as tightly packed RGBA8888
 * pixels, row by row from the top-left corner.
 *
 * <p>Use this when you need to build or edit pixels on the CPU: generating a solid rectangle for
 * {@code FlixelSprite.makeGraphic}, assembling a font glyph atlas, or holding freshly decoded
 * image data before upload. When the pixels are ready, turn them into a GPU texture with
 * {@link FlixelGraphicsManager#createTexture(FlixelImage)}.
 *
 * <p>Decoding an encoded file (PNG, JPEG, and so on) into one of these is a backend service; go
 * through {@link FlixelGraphicsManager#decodeImage(ByteBuffer)} or, more commonly, just load the
 * file through the asset manager and let it do the work.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelImage image = new FlixelImage(64, 64);
 * image.fill(FlixelColor.RED);
 * FlixelTexture texture = Flixel.graphics.createTexture(image);
 * }</pre>
 */
public final class FlixelImage {

  private final int width;
  private final int height;

  @NotNull
  private final ByteBuffer pixels;

  /**
   * Creates a blank, fully transparent image.
   *
   * @param width Width in pixels; must be positive.
   * @param height Height in pixels; must be positive.
   */
  public FlixelImage(int width, int height) {
    this(width, height, ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder()));
  }

  /**
   * Wraps existing RGBA8888 pixel data without copying it.
   *
   * @param width Width in pixels; must be positive.
   * @param height Height in pixels; must be positive.
   * @param pixels Tightly packed RGBA pixels; must hold at least {@code width * height * 4} bytes.
   */
  public FlixelImage(int width, int height, @NotNull ByteBuffer pixels) {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Image size must be positive, got " + width + "x" + height + ".");
    }
    this.width = width;
    this.height = height;
    this.pixels = pixels;
  }

  /**
   * Fills the whole image with one color.
   *
   * @param color The fill color.
   */
  public void fill(@NotNull FlixelColor color) {
    fill(color.getColor());
  }

  /**
   * Fills the whole image with one packed color.
   *
   * @param rgba8888 The fill color as packed RGBA8888.
   */
  public void fill(int rgba8888) {
    byte r = (byte) (rgba8888 >>> 24);
    byte g = (byte) (rgba8888 >>> 16);
    byte b = (byte) (rgba8888 >>> 8);
    byte a = (byte) rgba8888;
    int count = width * height;
    for (int i = 0; i < count; i++) {
      int o = i * 4;
      pixels.put(o, r);
      pixels.put(o + 1, g);
      pixels.put(o + 2, b);
      pixels.put(o + 3, a);
    }
  }

  /**
   * Sets one pixel. Coordinates outside the image are ignored.
   *
   * @param x Horizontal position from the left edge.
   * @param y Vertical position from the top edge.
   * @param rgba8888 The color as packed RGBA8888.
   */
  public void setPixel(int x, int y, int rgba8888) {
    if (x < 0 || y < 0 || x >= width || y >= height) {
      return;
    }
    int o = (y * width + x) * 4;
    pixels.put(o, (byte) (rgba8888 >>> 24));
    pixels.put(o + 1, (byte) (rgba8888 >>> 16));
    pixels.put(o + 2, (byte) (rgba8888 >>> 8));
    pixels.put(o + 3, (byte) rgba8888);
  }

  /**
   * Reads one pixel. Coordinates outside the image return {@code 0} (fully transparent black).
   *
   * @param x Horizontal position from the left edge.
   * @param y Vertical position from the top edge.
   * @return The color as packed RGBA8888.
   */
  public int getPixel(int x, int y) {
    if (x < 0 || y < 0 || x >= width || y >= height) {
      return 0;
    }
    int o = (y * width + x) * 4;
    return ((pixels.get(o) & 0xFF) << 24)
        | ((pixels.get(o + 1) & 0xFF) << 16)
        | ((pixels.get(o + 2) & 0xFF) << 8)
        | (pixels.get(o + 3) & 0xFF);
  }

  /**
   * Copies another image into this one at the given position, clipping at the edges. No blending
   * is performed; source pixels overwrite destination pixels.
   *
   * @param source The image to copy from.
   * @param x Left edge of the destination rectangle.
   * @param y Top edge of the destination rectangle.
   */
  public void draw(@NotNull FlixelImage source, int x, int y) {
    int copyW = Math.min(source.width, width - x);
    int copyH = Math.min(source.height, height - y);
    for (int row = 0; row < copyH; row++) {
      int dy = y + row;
      if (dy < 0) {
        continue;
      }
      for (int col = 0; col < copyW; col++) {
        int dx = x + col;
        if (dx < 0) {
          continue;
        }
        int so = (row * source.width + col) * 4;
        int dst = (dy * width + dx) * 4;
        pixels.put(dst, source.pixels.get(so));
        pixels.put(dst + 1, source.pixels.get(so + 1));
        pixels.put(dst + 2, source.pixels.get(so + 2));
        pixels.put(dst + 3, source.pixels.get(so + 3));
      }
    }
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  /**
   * Returns the backing pixel buffer for direct access (decoding, uploads).
   *
   * <p>The buffer's position and limit are not part of this class's contract; callers that
   * iterate should use absolute (indexed) reads and writes.
   *
   * @return The RGBA8888 pixel buffer; never {@code null}.
   */
  @NotNull
  public ByteBuffer getPixels() {
    return pixels;
  }
}
