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
package org.flixelgdx.backend.desktop.text;

import org.flixelgdx.text.FlixelFontRasterizer;
import org.flixelgdx.text.FlixelRasterizedFont;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The desktop font rasterizer, built on stb_truetype.
 *
 * <p>stb_truetype reads TrueType ({@code .ttf}) and OpenType ({@code .otf}) outlines and renders
 * individual glyphs to 8-bit coverage bitmaps. This class installs itself on the font registry so
 * scalable fonts bake into glyph atlases the same way on desktop as any other platform.
 *
 * @see FlixelStbRasterizedFont
 */
public class FlixelStbFontRasterizer implements FlixelFontRasterizer {

  @Nullable
  @Override
  public FlixelRasterizedFont open(byte @NotNull [] data, float pixelHeight) {
    return FlixelStbRasterizedFont.open(data, pixelHeight);
  }
}
