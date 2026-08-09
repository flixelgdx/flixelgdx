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

import org.flixelgdx.graphics.FlixelFrame;
import org.jetbrains.annotations.Nullable;

/**
 * One character of a baked {@link FlixelFont}: where its pixels live in the atlas and how it
 * sits on a text line.
 *
 * <p>All values are in the font's baked pixels. {@code xOffset}/{@code yOffset} position the
 * glyph's rectangle relative to the pen: {@code xOffset} to the right of the pen,
 * {@code yOffset} down from the top of the line. {@code xAdvance} moves the pen for the next
 * glyph.
 */
public final class FlixelGlyph {

  /** Horizontal pen advance to the next glyph, in baked pixels. */
  public float xAdvance;

  /** Offset from the pen to the glyph rectangle's left edge, in baked pixels. */
  public float xOffset;

  /** Offset from the line's top to the glyph rectangle's top edge, in baked pixels. */
  public float yOffset;

  /** Left edge of the glyph's rectangle inside its atlas page, in pixels. */
  public int x;

  /** Top edge of the glyph's rectangle inside its atlas page, in pixels. */
  public int y;

  /** Width of the glyph's rectangle, in pixels; {@code 0} for blank glyphs such as space. */
  public int width;

  /** Height of the glyph's rectangle, in pixels; {@code 0} for blank glyphs such as space. */
  public int height;

  /** Index of the atlas page holding this glyph's pixels. */
  public int page;

  /** The frame used to draw this glyph, created once when the font's pages are ready. */
  @Nullable
  FlixelFrame frame;
}
