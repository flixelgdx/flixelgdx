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

import org.flixelgdx.backend.gpu.FlixelBasisTranscoder;
import org.flixelgdx.backend.gpu.FlixelGpuBlockFormat;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * The Basis Universal transcoder on Android, backed by the native {@code libflixel_basisu.so} library.
 *
 * <p>The launcher installs one with {@code FlixelGpuGraphics.setBasisTranscoder(...)}, which is what lets mobile
 * games ship supercompressed {@code .ktx2} textures: small in the APK, and still compressed in GPU memory once
 * transcoded to ASTC or ETC2. Creating the first instance loads the library.
 *
 * <p>Handles are independent, so several asset loading threads can transcode at the same time.
 */
public final class FlixelAndroidBasisTranscoder implements FlixelBasisTranscoder {

  // Target numbers shared with flixel_basisu.cpp.
  private static final int TARGET_ASTC_4X4 = 0;
  private static final int TARGET_ETC2_RGBA8 = 1;
  private static final int TARGET_ETC2_RGB8 = 2;
  private static final int TARGET_BC7 = 3;
  private static final int TARGET_RGBA8 = 4;

  static {
    System.loadLibrary("flixel_basisu");
    nInit();
  }

  @Override
  public long open(@NotNull ByteBuffer container) {
    if (!container.isDirect()) {
      return 0L;
    }
    return nOpen(container, container.limit());
  }

  @Override
  public void close(long handle) {
    if (handle != 0L) {
      nClose(handle);
    }
  }

  @Override
  public int getLevels(long handle) {
    return nGetLevels(handle);
  }

  @Override
  public int getWidth(long handle) {
    return nGetWidth(handle);
  }

  @Override
  public int getHeight(long handle) {
    return nGetHeight(handle);
  }

  @Override
  public boolean hasAlpha(long handle) {
    return nHasAlpha(handle);
  }

  @Override
  public boolean isUastc(long handle) {
    return nIsUastc(handle);
  }

  @Override
  public boolean transcode(long handle, int level, @NotNull FlixelGpuBlockFormat target, @NotNull ByteBuffer output) {
    int id = switch (target) {
      case ASTC_4X4 -> TARGET_ASTC_4X4;
      case ETC2_RGBA8 -> TARGET_ETC2_RGBA8;
      case ETC2_RGB8 -> TARGET_ETC2_RGB8;
      case BC7 -> TARGET_BC7;
      case RGBA8 -> TARGET_RGBA8;
      default -> -1;
    };
    if (id < 0 || !output.isDirect()) {
      return false;
    }
    return nTranscode(handle, level, id, output, output.position(), output.remaining());
  }

  private static native void nInit();

  private static native long nOpen(ByteBuffer buffer, int length);

  private static native void nClose(long handle);

  private static native int nGetLevels(long handle);

  private static native int nGetWidth(long handle);

  private static native int nGetHeight(long handle);

  private static native boolean nHasAlpha(long handle);

  private static native boolean nIsUastc(long handle);

  private static native boolean nTranscode(long handle, int level, int target, ByteBuffer buffer, int offset,
      int size);
}
