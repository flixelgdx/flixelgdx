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
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Reads KTX2 texture files, the standard container for GPU textures.
 *
 * <p>A KTX2 file holds a texture already in a GPU format, with all of its mip levels, so loading it skips the slow
 * PNG decode and the texture stays compressed in GPU memory. Two kinds of payload are supported:
 *
 * <ul>
 *   <li><b>A GPU format</b> such as ASTC, ETC2, BC7 or RGBA8 (see {@link FlixelGpuBlockFormat}). The levels are
 *       uploaded exactly as stored, which works on any platform whose GPU supports that format. These files cannot
 *       use supercompression.</li>
 *   <li><b>Basis Universal</b>, in its small ETC1S or its high quality UASTC encoding, optionally supercompressed
 *       with Zstandard. This is what the FlixelGDX Basis Universal Gradle plugin produces. A platform transcoder
 *       ({@link FlixelBasisTranscoder}) converts it to the best format the GPU supports, falling back to RGBA8 on a
 *       GPU without any compressed format.</li>
 * </ul>
 *
 * <p>Only plain 2D textures are supported: no arrays, cube maps or 3D textures.
 *
 * <p>Most games never use this class: the Android backend loads {@code .ktx2} files through the asset manager. A
 * header can be inspected directly:
 *
 * <pre>{@code
 * FlixelKtx2 header = FlixelKtx2.parse(bytes);
 * if (header != null && header.isBasis()) {
 *   // A Basis Universal file that needs a transcoder.
 * }
 * }</pre>
 */
public final class FlixelKtx2 {

  /** No supercompression: the levels are stored as the GPU reads them. */
  public static final int SUPERCOMPRESSION_NONE = 0;

  /** BasisLZ supercompression, used by Basis Universal's ETC1S encoding. */
  public static final int SUPERCOMPRESSION_BASIS_LZ = 1;

  /** Zstandard supercompression. */
  public static final int SUPERCOMPRESSION_ZSTD = 2;

  /** The most mip levels a file may have, enough for a 32768 pixel wide texture. */
  public static final int MAX_LEVELS = 16;

  private static final int HEADER_BYTES = 80;
  private static final int LEVEL_ENTRY_BYTES = 24;
  private static final int MODEL_UASTC = 166;
  private static final int[] MAGIC = { 0xAB, 0x4B, 0x54, 0x58, 0x20, 0x32, 0x30, 0xBB, 0x0D, 0x0A, 0x1A, 0x0A };

  private final long[] levelOffsets;
  private final long[] levelLengths;
  private final int vkFormat;
  private final int width;
  private final int height;
  private final int supercompression;
  private final int colorModel;

  private FlixelKtx2(int vkFormat, int width, int height, int supercompression, int colorModel, long[] levelOffsets,
      long[] levelLengths) {
    this.vkFormat = vkFormat;
    this.width = width;
    this.height = height;
    this.supercompression = supercompression;
    this.colorModel = colorModel;
    this.levelOffsets = levelOffsets;
    this.levelLengths = levelLengths;
  }

  /**
   * Reads the header and level index of a KTX2 file.
   *
   * <p>The buffer is read from index 0 to its limit, with absolute reads, so its position and byte order do not
   * matter and are left unchanged.
   *
   * @param data The whole file.
   * @return The header, or {@code null} when the data is not a KTX2 file, is cut short, or is not a plain 2D
   *     texture.
   */
  @Nullable
  public static FlixelKtx2 parse(@NotNull ByteBuffer data) {
    int size = data.limit();
    if (size < HEADER_BYTES) {
      return null;
    }
    for (int i = 0; i < MAGIC.length; i++) {
      if ((data.get(i) & 0xFF) != MAGIC[i]) {
        return null;
      }
    }
    int vkFormat = u32(data, 12);
    int width = u32(data, 20);
    int height = u32(data, 24);
    int depth = u32(data, 28);
    int layers = u32(data, 32);
    int faces = u32(data, 36);
    int levels = Math.max(1, u32(data, 40));
    int supercompression = u32(data, 44);
    if (width <= 0 || height <= 0 || depth != 0 || layers > 1 || layers < 0 || faces != 1 || levels > MAX_LEVELS) {
      return null;
    }
    if (HEADER_BYTES + levels * LEVEL_ENTRY_BYTES > size) {
      return null;
    }
    long[] offsets = new long[levels];
    long[] lengths = new long[levels];
    for (int i = 0; i < levels; i++) {
      int entry = HEADER_BYTES + i * LEVEL_ENTRY_BYTES;
      long offset = u64(data, entry);
      long length = u64(data, entry + 8);
      if (offset < 0L || length < 0L || offset + length > size) {
        return null;
      }
      offsets[i] = offset;
      lengths[i] = length;
    }
    // The data format descriptor starts with its total size, then the basic block's three header words. The color
    // model is the low byte of the third word, and tells UASTC apart from ETC1S and plain formats.
    int colorModel = -1;
    int dfdOffset = u32(data, 48);
    int dfdLength = u32(data, 52);
    if (dfdOffset > 0 && dfdLength >= 16 && dfdOffset + 16 <= size) {
      colorModel = data.get(dfdOffset + 12) & 0xFF;
    }
    return new FlixelKtx2(vkFormat, width, height, supercompression, colorModel, offsets, lengths);
  }

  /**
   * Turns a KTX2 file into an image that is ready to upload.
   *
   * <p>This only uses the CPU, so it may run on any thread.
   *
   * @param container The whole file. A Basis Universal file is copied into a direct buffer first when this one is
   *     not direct; a file in a GPU format is uploaded straight from this buffer, which must then stay unchanged
   *     until the upload.
   * @param supported The formats the GPU supports, as a mask of {@link FlixelGpuBlockFormat#bit()} values.
   * @param transcoder The Basis Universal transcoder, or {@code null} when the platform has none.
   * @return The image.
   * @throws IllegalArgumentException When the file is invalid, uses a format this GPU does not support, or needs a
   *     transcoder the platform does not have. The message says which.
   */
  @NotNull
  public static FlixelGpuCompressedImage decode(@NotNull ByteBuffer container, int supported,
      @Nullable FlixelBasisTranscoder transcoder) {
    FlixelKtx2 header = parse(container);
    if (header == null) {
      throw new IllegalArgumentException("The data is not a valid 2D KTX2 texture.");
    }
    return header.isBasis() ? header.transcode(container, supported, transcoder) : header.slice(container, supported);
  }

  /**
   * Picks the GPU format a Basis Universal file is transcoded to.
   *
   * <p>UASTC prefers ASTC, which it converts to without any loss, and then BC7. ETC1S prefers ETC2, which it
   * converts to without any loss, using the half-size opaque variant when the image has no alpha. RGBA8 is the last
   * resort and always works.
   *
   * @param uastc Whether the file uses the UASTC encoding.
   * @param alpha Whether the image has an alpha channel.
   * @param supported The formats the GPU supports, as a mask of {@link FlixelGpuBlockFormat#bit()} values.
   * @return The format to transcode to.
   */
  @NotNull
  public static FlixelGpuBlockFormat chooseBasisTarget(boolean uastc, boolean alpha, int supported) {
    FlixelGpuBlockFormat etc2 = alpha ? FlixelGpuBlockFormat.ETC2_RGBA8 : FlixelGpuBlockFormat.ETC2_RGB8;
    if (uastc) {
      if (has(supported, FlixelGpuBlockFormat.ASTC_4X4)) {
        return FlixelGpuBlockFormat.ASTC_4X4;
      }
      if (has(supported, FlixelGpuBlockFormat.BC7)) {
        return FlixelGpuBlockFormat.BC7;
      }
      if (has(supported, etc2)) {
        return etc2;
      }
      return FlixelGpuBlockFormat.RGBA8;
    }
    if (has(supported, etc2)) {
      return etc2;
    }
    if (has(supported, FlixelGpuBlockFormat.ASTC_4X4)) {
      return FlixelGpuBlockFormat.ASTC_4X4;
    }
    if (has(supported, FlixelGpuBlockFormat.BC7)) {
      return FlixelGpuBlockFormat.BC7;
    }
    return FlixelGpuBlockFormat.RGBA8;
  }

  /**
   * Counts how many mip levels can be uploaded once the texture is rounded up to whole blocks.
   *
   * <p>A compressed texture must be a whole number of blocks wide and high, so a 50 pixel wide image becomes a 52
   * pixel wide texture. The file's smaller levels were made from the original 50 pixels, though, and halving 52 does
   * not always give the same number of blocks as halving 50 (at a quarter, 13 pixels need 4 blocks but 12 pixels need
   * 3). Only the levels where both agree are kept, which is every level when the size is already a multiple of 4.
   *
   * @param format The format of the texture.
   * @param width The original width in pixels.
   * @param height The original height in pixels.
   * @param levels The number of levels in the file.
   * @return The number of levels to upload, at least 1.
   */
  public static int usableLevels(@NotNull FlixelGpuBlockFormat format, int width, int height, int levels) {
    int paddedWidth = format.padded(width);
    int paddedHeight = format.padded(height);
    int limit = Math.min(levels, 32 - Integer.numberOfLeadingZeros(Math.max(paddedWidth, paddedHeight)));
    int count = 1;
    for (int level = 1; level < limit; level++) {
      boolean sameWidth = format.blocks(paddedWidth >> level) == format.blocks(width >> level);
      boolean sameHeight = format.blocks(paddedHeight >> level) == format.blocks(height >> level);
      if (!sameWidth || !sameHeight) {
        break;
      }
      count++;
    }
    return count;
  }

  private FlixelGpuCompressedImage slice(ByteBuffer container, int supported) {
    FlixelGpuBlockFormat format = FlixelGpuBlockFormat.fromVkFormat(vkFormat);
    if (format == null) {
      throw new IllegalArgumentException("The KTX2 pixel format (Vulkan format " + vkFormat + ") is not supported.");
    }
    if (supercompression != SUPERCOMPRESSION_NONE) {
      throw new IllegalArgumentException("Only Basis Universal KTX2 files can be supercompressed, but this "
          + format + " file uses supercompression scheme " + supercompression + ".");
    }
    if (!has(supported, format)) {
      throw new IllegalArgumentException("This GPU does not support " + format + " textures.");
    }
    int levels = usableLevels(format, width, height, levelOffsets.length);
    int[] offsets = new int[levels];
    int[] sizes = new int[levels];
    for (int level = 0; level < levels; level++) {
      int expected = format.levelBytes(width >> level, height >> level);
      if (levelLengths[level] != expected) {
        throw new IllegalArgumentException("Level " + level + " of the KTX2 file has " + levelLengths[level]
            + " bytes, but a " + format + " level of that size needs " + expected + ".");
      }
      offsets[level] = (int) levelOffsets[level];
      sizes[level] = expected;
    }
    return new FlixelGpuCompressedImage(container, format, width, height, offsets, sizes);
  }

  private FlixelGpuCompressedImage transcode(ByteBuffer container, int supported,
      @Nullable FlixelBasisTranscoder transcoder) {
    if (transcoder == null) {
      throw new IllegalArgumentException("This is a Basis Universal KTX2 file, and this platform has no Basis"
          + " Universal transcoder to convert it for the GPU.");
    }
    ByteBuffer input = container;
    if (!input.isDirect()) {
      input = ByteBuffer.allocateDirect(container.limit());
      input.put(container.duplicate().position(0)).clear();
    }
    long handle = transcoder.open(input);
    if (handle == 0L) {
      throw new IllegalArgumentException("The Basis Universal data in the KTX2 file is invalid.");
    }
    try {
      int w = transcoder.getWidth(handle);
      int h = transcoder.getHeight(handle);
      FlixelGpuBlockFormat target = chooseBasisTarget(transcoder.isUastc(handle), transcoder.hasAlpha(handle),
          supported);
      int levels = usableLevels(target, w, h, transcoder.getLevels(handle));
      int[] offsets = new int[levels];
      int[] sizes = new int[levels];
      long total = 0L;
      for (int level = 0; level < levels; level++) {
        offsets[level] = (int) total;
        sizes[level] = target.levelBytes(w >> level, h >> level);
        total += sizes[level];
      }
      if (total > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("The texture is too large (" + w + "x" + h + ").");
      }
      ByteBuffer output = ByteBuffer.allocateDirect((int) total).order(ByteOrder.nativeOrder());
      for (int level = 0; level < levels; level++) {
        output.limit(offsets[level] + sizes[level]).position(offsets[level]);
        if (!transcoder.transcode(handle, level, target, output)) {
          throw new IllegalArgumentException("Level " + level + " could not be transcoded to " + target + ".");
        }
      }
      output.clear();
      return new FlixelGpuCompressedImage(output, target, w, h, offsets, sizes);
    } finally {
      transcoder.close(handle);
    }
  }

  private static boolean has(int supported, FlixelGpuBlockFormat format) {
    return (supported & format.bit()) != 0;
  }

  private static int u32(ByteBuffer data, int at) {
    return (data.get(at) & 0xFF) | (data.get(at + 1) & 0xFF) << 8 | (data.get(at + 2) & 0xFF) << 16
        | (data.get(at + 3) & 0xFF) << 24;
  }

  private static long u64(ByteBuffer data, int at) {
    return (u32(data, at) & 0xFFFFFFFFL) | (long) u32(data, at + 4) << 32;
  }

  /**
   * Returns whether the payload is Basis Universal data that needs a transcoder.
   *
   * @return {@code true} for ETC1S (BasisLZ) and UASTC files.
   */
  public boolean isBasis() {
    return vkFormat == 0 && (supercompression == SUPERCOMPRESSION_BASIS_LZ || colorModel == MODEL_UASTC);
  }

  /**
   * Returns whether the payload is Basis Universal's UASTC encoding.
   *
   * @return {@code true} for UASTC files, supercompressed or not.
   */
  public boolean isUastc() {
    return vkFormat == 0 && colorModel == MODEL_UASTC;
  }

  /**
   * Returns the byte offset of a level in the file.
   *
   * @param level The level, where 0 is the largest.
   * @return The offset in bytes.
   */
  public long getLevelOffset(int level) {
    return levelOffsets[level];
  }

  /**
   * Returns the stored byte length of a level, after any supercompression.
   *
   * @param level The level, where 0 is the largest.
   * @return The length in bytes.
   */
  public long getLevelLength(int level) {
    return levelLengths[level];
  }

  /**
   * Returns the Vulkan format number of the payload.
   *
   * @return The format, or 0 for Basis Universal files.
   */
  public int getVkFormat() {
    return vkFormat;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public int getLevels() {
    return levelOffsets.length;
  }

  /**
   * Returns the supercompression scheme.
   *
   * @return One of the {@code SUPERCOMPRESSION_} constants, or another scheme number from the KTX2 specification.
   */
  public int getSupercompression() {
    return supercompression;
  }
}
