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

import org.flixelgdx.backend.gpu.FlixelImageDecoder;
import org.flixelgdx.graphics.FlixelImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Decodes encoded image files (PNG, JPEG, WebP and so on) with Android's own bitmap decoder.
 *
 * <p>The shared renderer asks the platform to turn file bytes into RGBA pixels, because decoding is the one part of
 * loading a texture that differs per platform. The decoder keeps the colors un-premultiplied and unscaled, so the
 * pixels match the file exactly no matter the density of the screen. It is safe to call from any thread, which lets
 * the asset manager decode on its worker threads.
 */
public final class FlixelAndroidImageDecoder implements FlixelImageDecoder {

  @Nullable
  @Override
  public FlixelImage decode(@NotNull ByteBuffer encoded) {
    // BitmapFactory needs a byte array (or a seekable stream), so copy the encoded bytes out of the buffer.
    byte[] bytes = new byte[encoded.remaining()];
    encoded.duplicate().get(bytes);

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    options.inPremultiplied = false;
    options.inScaled = false;
    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    if (bitmap == null) {
      return null;
    }

    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
    bitmap.copyPixelsToBuffer(pixels);
    bitmap.recycle();
    pixels.rewind();
    return new FlixelImage(width, height, pixels);
  }
}
