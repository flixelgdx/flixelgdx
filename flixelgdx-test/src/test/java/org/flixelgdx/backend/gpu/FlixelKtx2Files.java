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
package org.flixelgdx.backend.gpu;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Builds KTX2 files in memory and reads the encoded fixtures, for the KTX2 tests. */
final class FlixelKtx2Files {

  private FlixelKtx2Files() {}

  /**
   * Reads a fixture made by the Basis Universal encoder (a 50x37 image with 6 mip levels).
   *
   * @param name The file name in this package's test resources.
   * @return The file, in a direct buffer.
   */
  static ByteBuffer fixture(String name) {
    try (InputStream in = FlixelKtx2Files.class.getResourceAsStream(name)) {
      if (in == null) {
        throw new IllegalStateException("Missing test fixture " + name);
      }
      byte[] bytes = in.readAllBytes();
      ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
      buffer.put(bytes).flip();
      return buffer;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Builds a KTX2 file whose levels hold the right number of bytes for the format, each filled with its level
   * number plus one.
   *
   * @param format The format of the payload.
   * @param width The base width.
   * @param height The base height.
   * @param levels The number of levels.
   * @param supercompression The supercompression scheme to write in the header.
   * @return The file.
   */
  static ByteBuffer raw(@NotNull FlixelGpuBlockFormat format, int width, int height, int levels,
      int supercompression) {
    int[] sizes = new int[levels];
    for (int i = 0; i < levels; i++) {
      sizes[i] = format.levelBytes(width >> i, height >> i);
    }
    return build(vkFormat(format), width, height, levels, supercompression, sizes);
  }

  /**
   * Builds a KTX2 file with explicit level sizes.
   *
   * @return The file, with no data format descriptor.
   */
  static ByteBuffer build(int vkFormat, int width, int height, int levels, int supercompression, int[] sizes) {
    int header = 80 + levels * 24;
    int total = header;
    for (int size : sizes) {
      total += size;
    }
    ByteBuffer b = ByteBuffer.allocateDirect(total).order(ByteOrder.LITTLE_ENDIAN);
    b.put(new byte[] { (byte) 0xAB, 'K', 'T', 'X', ' ', '2', '0', (byte) 0xBB, '\r', '\n', 0x1A, '\n' });
    b.putInt(vkFormat).putInt(1).putInt(width).putInt(height).putInt(0).putInt(0).putInt(1).putInt(levels)
        .putInt(supercompression);
    b.putInt(0).putInt(0).putInt(0).putInt(0).putLong(0L).putLong(0L);
    int offset = header;
    for (int i = 0; i < levels; i++) {
      b.putLong(offset).putLong(sizes[i]).putLong(sizes[i]);
      offset += sizes[i];
    }
    for (int i = 0; i < levels; i++) {
      for (int j = 0; j < sizes[i]; j++) {
        b.put((byte) (i + 1));
      }
    }
    return b.flip();
  }

  /** Returns the Vulkan UNORM format number of a block format. */
  static int vkFormat(@NotNull FlixelGpuBlockFormat format) {
    return switch (format) {
      case ASTC_4X4 -> 157;
      case ETC2_RGBA8 -> 151;
      case ETC2_RGB8 -> 147;
      case BC7 -> 145;
      case BC3 -> 137;
      case BC1 -> 133;
      case RGBA8 -> 37;
    };
  }
}
