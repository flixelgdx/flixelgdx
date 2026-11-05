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

import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.graphics.FlixelDisplayMode;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelNoopGraphicsManager;
import org.flixelgdx.graphics.FlixelPixelReadback;
import org.flixelgdx.graphics.FlixelTexture;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/** Test doubles for the texture panel and the thumbnail queue. */
final class ReadbackTestSupport {

  private ReadbackTestSupport() {}

  /** A graphics manager whose pixel readback is answered by the test, when it decides to. */
  static final class ReadbackGraphics implements FlixelGraphicsManager {

    FlixelTexture lastTexture;
    FlixelPixelReadback lastCallback;
    int lastX;
    int lastY;
    int lastWidth;
    int lastHeight;
    int requests;
    boolean supported = true;

    @Override
    public FlixelList<FlixelDisplayMode> getDisplayModes() {
      return FlixelNoopGraphicsManager.INSTANCE.getDisplayModes();
    }

    @Override
    public boolean supportsReadPixels() {
      return supported;
    }

    @Override
    public void readPixels(@NotNull FlixelTexture texture, int x, int y, int width, int height,
        @NotNull FlixelPixelReadback callback) {
      requests++;
      lastTexture = texture;
      lastX = x;
      lastY = y;
      lastWidth = width;
      lastHeight = height;
      lastCallback = callback;
    }

    /** Answers the open request with a picture of the size that was asked for, with padded rows. */
    void answer(int rowPadding) {
      int rowBytes = lastWidth * 4 + rowPadding;
      ByteBuffer pixels = ByteBuffer.allocate(rowBytes * lastHeight);
      for (int y = 0; y < lastHeight; y++) {
        for (int x = 0; x < lastWidth; x++) {
          int i = y * rowBytes + x * 4;
          pixels.put(i, (byte) 255);
          pixels.put(i + 1, (byte) (x % 256));
          pixels.put(i + 2, (byte) (y % 256));
          pixels.put(i + 3, (byte) 255);
        }
      }
      lastCallback.onPixels(pixels, lastWidth, lastHeight, rowBytes);
    }

    void fail() {
      lastCallback.onPixels(null, 0, 0, 0);
    }
  }

  /** A texture with a made-up size, memory use, and format. */
  static final class FakeTexture implements FlixelTexture {

    final int width;
    final int height;
    final long bytes;
    final String format;

    boolean smooth;

    FakeTexture(int width, int height) {
      this(width, height, (long) width * height * 4L, "RGBA8");
    }

    FakeTexture(int width, int height, long bytes, String format) {
      this.width = width;
      this.height = height;
      this.bytes = bytes;
      this.format = format;
    }

    @Override
    public long getHandle() {
      return 1L;
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
    public long getByteSize() {
      return bytes;
    }

    @NotNull
    @Override
    public String getFormat() {
      return format;
    }

    @Override
    public boolean isSmooth() {
      return smooth;
    }

    @Override
    public void setSmooth(boolean smooth) {
      this.smooth = smooth;
    }

    @Override
    public void update(int x, int y, @NotNull FlixelImage image) {}

    @Override
    public void destroy() {}
  }
}
