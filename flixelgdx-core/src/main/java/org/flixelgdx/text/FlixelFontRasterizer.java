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
package org.flixelgdx.text;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The platform's font-file decoder: opens TrueType/OpenType data at a pixel size so glyphs can
 * be rasterized.
 *
 * <p>This is the seam that keeps scalable-font support cross-platform. The desktop backend
 * installs an stb_truetype-based implementation (which reads both {@code .ttf} and
 * {@code .otf}); web can install one built on browser text APIs. When no rasterizer is
 * installed, scalable fonts are unavailable and text falls back to the packaged bitmap font,
 * so text never crashes.
 *
 * <p>Install via {@link FlixelFontRegistry#setRasterizer(FlixelFontRasterizer)} before
 * {@link org.flixelgdx.Flixel#start Flixel.start}; backends do this for you.
 */
public interface FlixelFontRasterizer {

  /**
   * Opens font data at one pixel size.
   *
   * @param data The raw bytes of a {@code .ttf} or {@code .otf} file.
   * @param pixelHeight The size to rasterize at, in pixels per line.
   * @return The opened font, or {@code null} when the data is not a supported font format.
   */
  @Nullable
  FlixelRasterizedFont open(byte @NotNull [] data, float pixelHeight);
}
