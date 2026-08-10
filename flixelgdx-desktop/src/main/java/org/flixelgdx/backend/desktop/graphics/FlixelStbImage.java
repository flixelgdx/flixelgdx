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
package org.flixelgdx.backend.desktop.graphics;

import org.flixelgdx.graphics.FlixelImage;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * Decodes encoded image files (PNG, JPEG, BMP, TGA, and more) into RGBA {@link FlixelImage}s using
 * stb_image.
 *
 * <p>This is the desktop backend's implementation of image decoding, called by the graphics
 * manager's {@code decodeImage}. The decoded pixels are copied into a Java-owned direct buffer so
 * the caller does not have to track stb's native allocation.
 */
final class FlixelStbImage {

  private FlixelStbImage() {}

  /**
   * Decodes an encoded image into a fresh RGBA image.
   *
   * @param encoded The encoded file bytes; must be a direct buffer positioned at the data start.
   * @return The decoded image, or {@code null} when the data is not a supported image.
   */
  @Nullable
  static FlixelImage decode(ByteBuffer encoded) {
    try (MemoryStack stack = MemoryStack.stackPush()) {
      IntBuffer w = stack.mallocInt(1);
      IntBuffer h = stack.mallocInt(1);
      IntBuffer channels = stack.mallocInt(1);
      // Force 4 channels (RGBA) so the layout always matches FlixelImage.
      ByteBuffer decoded = STBImage.stbi_load_from_memory(encoded, w, h, channels, 4);
      if (decoded == null) {
        return null;
      }
      try {
        int width = w.get(0);
        int height = h.get(0);
        ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
        pixels.put(decoded);
        pixels.flip();
        return new FlixelImage(width, height, pixels);
      } finally {
        // put(decoded) above advanced decoded's position to its limit. stbi_image_free frees the
        // address at the buffer's CURRENT position, so it would otherwise free base + capacity
        // rather than the allocation's start, aborting the process with an invalid free. Rewind
        // to position 0 first so the correct pointer is released.
        decoded.rewind();
        STBImage.stbi_image_free(decoded);
      }
    }
  }
}
