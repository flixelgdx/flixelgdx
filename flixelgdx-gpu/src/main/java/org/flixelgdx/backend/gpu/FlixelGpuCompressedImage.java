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

import java.nio.ByteBuffer;

/**
 * A texture that is ready to upload: its pixels in a GPU format, with every mip level.
 *
 * <p>This is the result of reading a KTX2 file with {@link FlixelGpuGraphics#prepareCompressedTexture}. Preparing
 * only uses the CPU, so it can run on an asset loading thread, and the upload in
 * {@link FlixelGpuGraphics#createTexture(FlixelGpuCompressedImage)} stays short.
 *
 * <p>All levels live in one buffer, one after the other. Level {@code i} starts at {@link #getOffset(int)} and is
 * {@link #getSize(int)} bytes long.
 */
public final class FlixelGpuCompressedImage {

  private final ByteBuffer data;
  private final FlixelGpuBlockFormat format;
  private final int[] offsets;
  private final int[] sizes;
  private final int width;
  private final int height;

  /**
   * Creates an image from data that is already laid out level after level.
   *
   * @param data The buffer that holds every level. It is not copied.
   * @param format The format of the data.
   * @param width The width of the largest level, in pixels (before rounding up to whole blocks).
   * @param height The height of the largest level, in pixels (before rounding up to whole blocks).
   * @param offsets Where each level starts in {@code data}. The array length is the level count.
   * @param sizes How many bytes each level takes.
   */
  public FlixelGpuCompressedImage(@NotNull ByteBuffer data, @NotNull FlixelGpuBlockFormat format, int width,
      int height, int @NotNull [] offsets, int @NotNull [] sizes) {
    if (offsets.length == 0 || offsets.length != sizes.length) {
      throw new IllegalArgumentException("An image needs at least one level, with one offset and size each.");
    }
    this.data = data;
    this.format = format;
    this.width = width;
    this.height = height;
    this.offsets = offsets;
    this.sizes = sizes;
  }

  /**
   * Returns the total number of bytes of every level.
   *
   * @return The size in bytes, which is what the texture takes in GPU memory.
   */
  public long getByteSize() {
    long total = 0L;
    for (int i = 0; i < sizes.length; i++) {
      total += sizes[i];
    }
    return total;
  }

  /**
   * Returns the width the GPU texture is created with: the width rounded up to whole blocks.
   *
   * @return The texture width in pixels.
   */
  public int getGpuWidth() {
    return format.padded(width);
  }

  /**
   * Returns the height the GPU texture is created with: the height rounded up to whole blocks.
   *
   * @return The texture height in pixels.
   */
  public int getGpuHeight() {
    return format.padded(height);
  }

  /**
   * Returns where a level starts in {@link #getData()}.
   *
   * @param level The level, where 0 is the largest.
   * @return The byte offset.
   */
  public int getOffset(int level) {
    return offsets[level];
  }

  /**
   * Returns how many bytes a level takes.
   *
   * @param level The level, where 0 is the largest.
   * @return The size in bytes.
   */
  public int getSize(int level) {
    return sizes[level];
  }

  @NotNull
  public ByteBuffer getData() {
    return data;
  }

  @NotNull
  public FlixelGpuBlockFormat getFormat() {
    return format;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public int getLevels() {
    return offsets.length;
  }
}
