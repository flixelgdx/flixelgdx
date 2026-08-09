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

import org.flixelgdx.graphics.FlixelImage;
import org.jetbrains.annotations.Nullable;

/**
 * One rasterized glyph: its pixels plus the metrics needed to place it on a text line.
 *
 * <p>Produced by a {@link FlixelRasterizedFont} while a {@link FlixelFont} bakes its atlas.
 * Coordinates follow the common font convention: {@link #bearingX} shifts the bitmap right of
 * the pen position, {@link #bearingY} is the distance from the baseline up to the bitmap's top
 * edge, and {@link #advance} moves the pen for the next glyph.
 */
public final class FlixelGlyphBitmap {

  /** The glyph's pixels, or {@code null} for blank glyphs such as the space character. */
  @Nullable
  public FlixelImage image;

  /** Horizontal pen advance to the next glyph, in pixels. */
  public float advance;

  /** Horizontal offset from the pen position to the bitmap's left edge, in pixels. */
  public float bearingX;

  /** Vertical distance from the baseline up to the bitmap's top edge, in pixels. */
  public float bearingY;
}
