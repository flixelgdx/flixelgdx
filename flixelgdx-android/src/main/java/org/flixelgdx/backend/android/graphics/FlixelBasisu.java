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
package org.flixelgdx.backend.android.graphics;

import java.nio.ByteBuffer;

import org.jetbrains.annotations.NotNull;

/**
 * Thin JNI binding to the Basis Universal KTX2 transcoder for the Android backend.
 *
 * <p>The static initializer calls {@link #init()} once so callers never need to do it
 * themselves. Every operation is driven through a native {@code long} handle obtained from
 * {@link #open(ByteBuffer)}; the handle holds a heap-allocated C++ object and must be
 * released by calling {@link #close(long)} when the caller no longer needs the transcoder.
 *
 * <p>Target format constants:
 * <ul>
 *   <li>{@link #FMT_ASTC_4x4_RGBA} - ASTC 4x4 with alpha, preferred on most modern Android
 *       GPUs (Adreno, Mali, PowerVR since 2013).</li>
 *   <li>{@link #FMT_ETC2_RGBA} - ETC2 EAC+alpha, universally supported on Android 4.3+
 *       (all devices that ship with OpenGL ES 3.0 or higher).</li>
 *   <li>{@link #FMT_RGBA32} - Uncompressed 32-bit RGBA, always available as a fallback.</li>
 * </ul>
 *
 * <p>Typical usage (layer 0, mip 0, ASTC target):
 * <pre>{@code
 * long handle = FlixelBasisu.open(ktx2Buffer);
 * if (handle == 0) { // handle failed parse }
 * int size = FlixelBasisu.getTranscodedSize(handle, 0, FlixelBasisu.FMT_ASTC_4x4_RGBA);
 * ByteBuffer out = ByteBuffer.allocateDirect(size);
 * boolean ok = FlixelBasisu.transcode(handle, 0, FlixelBasisu.FMT_ASTC_4x4_RGBA, out);
 * FlixelBasisu.close(handle);
 * }</pre>
 */
public final class FlixelBasisu {

  /** Target format constant: ASTC 4x4 RGBA (16 bytes per 4x4 block). */
  public static final int FMT_ASTC_4x4_RGBA = 0;

  /** Target format constant: ETC2 EAC RGBA (16 bytes per 4x4 block). */
  public static final int FMT_ETC2_RGBA = 1;

  /** Target format constant: uncompressed RGBA32 (4 bytes per pixel, R first, A last). */
  public static final int FMT_RGBA32 = 2;

  static {
    init();
  }

  private FlixelBasisu() {}

  /**
   * Initializes the global Basis Universal transcoder lookup tables.
   *
   * <p>The static initializer calls this automatically; there is no need to call it manually.
   * The underlying implementation is idempotent, so multiple calls are safe.
   */
  static native void init();

  /**
   * Opens a KTX2 file held in a direct {@link ByteBuffer} and returns a native handle.
   *
   * <p>The buffer must remain valid and unmodified for the lifetime of the handle. The method
   * calls both {@code ktx2_transcoder::init} and {@code start_transcoding} internally.
   *
   * @param buf a direct {@link ByteBuffer} containing the full KTX2 file data
   * @return a non-zero handle on success, or {@code 0} if {@code buf} is {@code null},
   *         is not a direct buffer, or the KTX2 data cannot be parsed
   */
  public static native long open(@NotNull ByteBuffer buf);

  /**
   * Releases native memory for a transcoder handle previously returned by {@link #open}.
   *
   * <p>Passing {@code 0} is a no-op. After this call the handle must not be used again.
   *
   * @param handle a handle returned by {@link #open}, or {@code 0}
   */
  public static native void close(long handle);

  /**
   * Returns the number of mip-map levels in the texture.
   *
   * @param handle a handle returned by {@link #open}
   * @return level count, or {@code 0} if the handle is invalid
   */
  public static native int getLevels(long handle);

  /**
   * Returns the base-level width of the texture in texels.
   *
   * @param handle a handle returned by {@link #open}
   * @return width in texels, or {@code 0} if the handle is invalid
   */
  public static native int getWidth(long handle);

  /**
   * Returns the base-level height of the texture in texels.
   *
   * @param handle a handle returned by {@link #open}
   * @return height in texels, or {@code 0} if the handle is invalid
   */
  public static native int getHeight(long handle);

  /**
   * Returns whether the texture has an alpha channel.
   *
   * @param handle a handle returned by {@link #open}
   * @return {@code true} if the texture carries alpha data
   */
  public static native boolean hasAlpha(long handle);

  /**
   * Returns whether the source encoding is UASTC (as opposed to ETC1S).
   *
   * <p>Both encodings can be transcoded to any of the three target formats; this accessor
   * is informational only.
   *
   * @param handle a handle returned by {@link #open}
   * @return {@code true} if the source encoding is UASTC
   */
  public static native boolean isUastc(long handle);

  /**
   * Returns the number of bytes required to hold the transcoded output for the given level
   * and target format.
   *
   * <p>Allocate or validate your output {@link ByteBuffer} with this value before calling
   * {@link #transcode}.
   *
   * @param handle     a handle returned by {@link #open}
   * @param levelIndex mip level index (0 = largest)
   * @param fmt        one of {@link #FMT_ASTC_4x4_RGBA}, {@link #FMT_ETC2_RGBA}, or
   *                   {@link #FMT_RGBA32}
   * @return byte count needed, or {@code 0} on error
   */
  public static native int getTranscodedSize(long handle, int levelIndex, int fmt);

  /**
   * Transcodes a single mip level into a caller-supplied direct {@link ByteBuffer}.
   *
   * <p>The output buffer must be at least {@link #getTranscodedSize} bytes and must have been
   * allocated with {@link ByteBuffer#allocateDirect}. The method writes from position 0
   * regardless of the buffer's current position or limit.
   *
   * @param handle     a handle returned by {@link #open}
   * @param levelIndex mip level index (0 = largest)
   * @param fmt        one of {@link #FMT_ASTC_4x4_RGBA}, {@link #FMT_ETC2_RGBA}, or
   *                   {@link #FMT_RGBA32}
   * @param outBuf     a direct {@link ByteBuffer} large enough to hold the output
   * @return {@code true} on success, {@code false} on any error
   */
  public static native boolean transcode(long handle, int levelIndex, int fmt,
      @NotNull ByteBuffer outBuf);
}
