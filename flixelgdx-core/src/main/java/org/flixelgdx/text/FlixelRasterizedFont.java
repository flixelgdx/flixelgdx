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

import org.flixelgdx.functional.FlixelDestroyable;
import org.jetbrains.annotations.Nullable;

/**
 * A font file opened at one pixel size, able to rasterize individual glyphs.
 *
 * <p>Produced by the platform's {@link FlixelFontRasterizer}. {@link FlixelFont} drives it
 * while baking a glyph atlas, then destroys it; game code never uses it directly.
 *
 * <p>All metrics are in pixels at the size the font was opened with. The vertical metrics
 * follow the usual convention: {@code ascent} is the distance from the baseline to the top of
 * the tallest glyph (positive), {@code descent} is the distance from the baseline to the
 * bottom of the lowest glyph (positive), and one line advances by {@link #getLineHeight()}.
 */
public interface FlixelRasterizedFont extends FlixelDestroyable {

  /**
   * Returns the distance from the baseline up to the top of the tallest glyph, in pixels.
   *
   * @return The ascent in pixels.
   */
  float getAscent();

  /**
   * Returns the distance from the baseline down to the bottom of the lowest glyph, in pixels
   * (positive).
   *
   * @return The descent in pixels, expressed as a positive value.
   */
  float getDescent();

  /**
   * Returns the vertical distance between two consecutive baselines, in pixels.
   *
   * @return The line height in pixels.
   */
  float getLineHeight();

  /**
   * Rasterizes one glyph.
   *
   * @param codepoint The Unicode codepoint to rasterize.
   * @return The glyph's pixels and metrics, or {@code null} when the font has no such glyph.
   */
  @Nullable
  FlixelGlyphBitmap rasterize(int codepoint);
}
