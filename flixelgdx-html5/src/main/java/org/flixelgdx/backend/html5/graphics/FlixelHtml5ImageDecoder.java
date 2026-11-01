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
package org.flixelgdx.backend.html5.graphics;

import org.flixelgdx.backend.gpu.FlixelImageDecoder;
import org.flixelgdx.graphics.FlixelImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Unpacks the images the browser has already decoded for the web backend.
 *
 * <p>A browser can only decode an encoded image (PNG, JPEG and so on) asynchronously, so it cannot happen inside
 * the synchronous {@link FlixelImageDecoder#decode(ByteBuffer)} call. The asset manager starts a
 * {@code createImageBitmap} promise for each image it is asked to load, and once the promise resolves it stores the
 * raw RGBA pixels behind a small {@code FLXI} header: the four magic bytes, then the width and the height as
 * little-endian 32-bit integers. This class only has to read that header and slice the pixels, which needs no real
 * decoding. Bytes without the header are a genuinely encoded image, which the web cannot decode synchronously, so
 * they come back as {@code null}.
 */
public final class FlixelHtml5ImageDecoder implements FlixelImageDecoder {

  /** The number of header bytes in front of the pixels: the magic plus the width and the height. */
  public static final int HEADER_SIZE = 12;

  /** The decoder to hand to the renderer. It holds no state, so one instance serves the whole game. */
  public static final FlixelHtml5ImageDecoder INSTANCE = new FlixelHtml5ImageDecoder();

  private FlixelHtml5ImageDecoder() {}

  @Override
  @Nullable
  public FlixelImage decode(@NotNull ByteBuffer encoded) {
    int base = encoded.position();
    if (encoded.remaining() < HEADER_SIZE
        || encoded.get(base) != 'F' || encoded.get(base + 1) != 'L'
        || encoded.get(base + 2) != 'X' || encoded.get(base + 3) != 'I') {
      return null;
    }
    int width = readInt(encoded, base + 4);
    int height = readInt(encoded, base + 8);
    if (width <= 0 || height <= 0) {
      return null;
    }
    int pixelBytes = width * height * 4;
    if (encoded.remaining() < HEADER_SIZE + pixelBytes) {
      return null;
    }
    // Slice the existing buffer instead of copying it. The renderer's upload path accepts both heap and direct
    // buffers, so no extra copy is needed here.
    ByteBuffer src = encoded.duplicate();
    src.position(base + HEADER_SIZE);
    src.limit(base + HEADER_SIZE + pixelBytes);
    return new FlixelImage(width, height, src.slice().order(ByteOrder.nativeOrder()));
  }

  /**
   * Reads a little-endian 32-bit integer from a byte buffer at an absolute offset.
   *
   * @param buffer The buffer to read from.
   * @param offset The absolute byte offset of the value.
   * @return The decoded integer.
   */
  private static int readInt(ByteBuffer buffer, int offset) {
    return (buffer.get(offset) & 0xFF)
        | ((buffer.get(offset + 1) & 0xFF) << 8)
        | ((buffer.get(offset + 2) & 0xFF) << 16)
        | ((buffer.get(offset + 3) & 0xFF) << 24);
  }
}
