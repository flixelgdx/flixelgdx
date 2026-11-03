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
 * Turns a Basis Universal KTX2 file into a block format the GPU can sample.
 *
 * <p>Basis Universal is a "supercompressed" format: the file is smaller than any GPU format, but no GPU can read it
 * directly. Before upload, a transcoder rewrites it into whichever format the device supports, such as ASTC or
 * ETC2. It is like a universal travel adapter: one plug in the file, and the transcoder fits it to whatever socket
 * the device has. Transcoding is fast, much faster than decoding a PNG, because the data is already arranged in
 * blocks.
 *
 * <p>The transcoder itself is native code, so a platform provides it and installs it with
 * {@link FlixelGpuGraphics#setBasisTranscoder}. The renderer decides the target format, sizes the output, and
 * calls these methods; a platform only forwards them to the native library.
 *
 * <p>Every method may be called from an asset loading thread, and several files may be open at once on different
 * threads, so an implementation must not keep shared mutable state between handles.
 *
 * <pre>{@code
 * long handle = transcoder.open(container);
 * if (handle != 0L) {
 *   try {
 *     int levels = transcoder.getLevels(handle);
 *     transcoder.transcode(handle, 0, FlixelGpuBlockFormat.ASTC_4X4, output);
 *   } finally {
 *     transcoder.close(handle);
 *   }
 * }
 * }</pre>
 */
public interface FlixelBasisTranscoder {

  /**
   * Opens a Basis Universal KTX2 file.
   *
   * <p>The buffer is read in place: it must stay unchanged until the handle is closed.
   *
   * @param container A direct buffer holding the whole file, from position 0 to its limit.
   * @return A handle for the other methods, or {@code 0} when the data is not a valid Basis Universal KTX2 file.
   */
  long open(@NotNull ByteBuffer container);

  /**
   * Releases a handle. Passing {@code 0} does nothing.
   *
   * @param handle A handle from {@link #open}, which must not be used afterwards.
   */
  void close(long handle);

  /**
   * Returns the number of mip levels in the file.
   *
   * @param handle A handle from {@link #open}.
   * @return The level count, at least 1.
   */
  int getLevels(long handle);

  /**
   * Returns the width of the largest mip level.
   *
   * @param handle A handle from {@link #open}.
   * @return The width in pixels.
   */
  int getWidth(long handle);

  /**
   * Returns the height of the largest mip level.
   *
   * @param handle A handle from {@link #open}.
   * @return The height in pixels.
   */
  int getHeight(long handle);

  /**
   * Returns whether the image has an alpha channel.
   *
   * @param handle A handle from {@link #open}.
   * @return {@code true} if any pixel can be transparent.
   */
  boolean hasAlpha(long handle);

  /**
   * Returns whether the file uses the high quality UASTC encoding rather than the smaller ETC1S one.
   *
   * <p>The renderer uses this to pick the target format that keeps the most quality: UASTC converts to ASTC
   * without any loss, and ETC1S converts to ETC2 without any loss.
   *
   * @param handle A handle from {@link #open}.
   * @return {@code true} for UASTC, {@code false} for ETC1S.
   */
  boolean isUastc(long handle);

  /**
   * Transcodes one mip level.
   *
   * <p>The output is written from the buffer's position, and needs exactly
   * {@link FlixelGpuBlockFormat#levelBytes(int, int)} bytes for the level's size. Block formats are written row by
   * row with no padding between rows, and {@link FlixelGpuBlockFormat#RGBA8} is written as RGBA pixels, also with no
   * padding. The buffer's position and limit are not changed.
   *
   * @param handle A handle from {@link #open}.
   * @param level The mip level, where 0 is the largest.
   * @param target The format to produce. It is never a format that cannot be produced from Basis Universal data
   *     ({@link FlixelGpuBlockFormat#BC1} and {@link FlixelGpuBlockFormat#BC3} are never asked for).
   * @param output A direct buffer with at least the level's size remaining.
   * @return {@code true} on success.
   */
  boolean transcode(long handle, int level, @NotNull FlixelGpuBlockFormat target, @NotNull ByteBuffer output);
}
