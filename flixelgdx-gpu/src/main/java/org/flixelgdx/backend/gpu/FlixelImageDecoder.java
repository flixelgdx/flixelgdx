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

import org.flixelgdx.graphics.FlixelImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * Turns the bytes of an encoded image file (PNG, JPEG and so on) into RGBA pixels.
 *
 * <p>Decoding is the one part of loading a texture that depends on the platform, so the shared renderer asks the
 * platform for it: the desktop uses stb_image, Android uses its bitmap decoder, and the web uses the browser.
 */
@FunctionalInterface
public interface FlixelImageDecoder {

  /**
   * Decodes an image.
   *
   * @param encoded The file bytes, positioned at the start of the data. Direct and heap buffers are both
   *     possible.
   * @return The decoded image, or {@code null} when the data is not a supported image.
   */
  @Nullable
  FlixelImage decode(@NotNull ByteBuffer encoded);
}
