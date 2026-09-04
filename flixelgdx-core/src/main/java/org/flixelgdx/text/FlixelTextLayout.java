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

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelFloatArray;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.util.FlixelAlign;
import org.jetbrains.annotations.NotNull;

/**
 * A reusable text layout: positions of every visible glyph for one string, ready to draw
 * repeatedly with no per-frame allocation.
 *
 * <p>{@link FlixelText} keeps one of these and rebuilds it only when the text, font, size, or
 * field settings change. Drawing the same layout at offset positions is how text borders are
 * rendered cheaply.
 *
 * <p>All output coordinates are in game pixels, measured from the text block's top-left
 * corner: {@code x} grows right and {@code y} grows down, matching the renderer's y-down space,
 * so the draw call adds the stored top offsets directly to the block's top edge.
 */
public final class FlixelTextLayout {

  @NotNull
  private final FlixelArray<FlixelFrame> frames = new FlixelArray<>(FlixelFrame[]::new, 64);

  @NotNull
  private final FlixelFloatArray xs = new FlixelFloatArray(64);

  /** Distance from the text block's top edge down to each glyph's top edge. */
  @NotNull
  private final FlixelFloatArray tops = new FlixelFloatArray(64);

  @NotNull
  private final FlixelFloatArray widths = new FlixelFloatArray(64);

  @NotNull
  private final FlixelFloatArray heights = new FlixelFloatArray(64);

  /** Index of the first glyph of each laid-out line, used for alignment shifting. */
  @NotNull
  private final FlixelFloatArray lineStarts = new FlixelFloatArray(8);

  private float width;
  private float height;

  /**
   * Rebuilds the layout.
   *
   * @param font The font to lay out with.
   * @param text The text to lay out.
   * @param scale Game pixels per baked font pixel ({@code 1} draws at the baked size).
   * @param fieldWidth Wrapping and alignment width in game pixels, or {@code 0} for natural width.
   * @param wrap {@code true} to wrap lines at {@code fieldWidth} (word-aware).
   * @param align One of {@link FlixelAlign#LEFT}, {@link FlixelAlign#CENTER}, {@link FlixelAlign#RIGHT}.
   * @param letterSpacing Extra pixels between characters, in game pixels.
   */
  public void set(@NotNull FlixelFont font, @NotNull CharSequence text, float scale,
      float fieldWidth, boolean wrap, int align, float letterSpacing) {
    frames.clear();
    xs.clear();
    tops.clear();
    widths.clear();
    heights.clear();
    lineStarts.clear();

    float lineHeight = font.getLineHeight() * scale;
    float penX = 0f;
    float lineTop = 0f;
    float maxLineWidth = 0f;
    int lineStart = 0;
    int lastSpaceIndex = -1;
    float lastSpacePenX = 0f;
    int length = text.length();

    lineStarts.add(0);
    for (int i = 0; i < length; i++) {
      char c = text.charAt(i);
      if (c == '\n') {
        maxLineWidth = Math.max(maxLineWidth, penX);
        alignLine(lineStart, frames.getSize(), penX, fieldWidth, align);
        penX = 0f;
        lineTop += lineHeight;
        lineStart = frames.getSize();
        lineStarts.add(lineStart);
        lastSpaceIndex = -1;
        continue;
      }
      FlixelGlyph glyph = font.getGlyph(c);
      if (glyph == null) {
        continue;
      }
      float advance = glyph.xAdvance * scale + letterSpacing;

      if (c == ' ') {
        lastSpaceIndex = frames.getSize();
        lastSpacePenX = penX;
      }

      // Word wrap: when this glyph would cross the field edge, move everything since the last
      // space down one line (or hard-break when a single word is wider than the field).
      if (wrap && fieldWidth > 0 && penX + advance > fieldWidth && penX > 0) {
        if (lastSpaceIndex >= 0 && lastSpaceIndex >= lineStart) {
          float shift = lastSpacePenX + spaceAdvanceAt(font, scale, letterSpacing);
          maxLineWidth = Math.max(maxLineWidth, lastSpacePenX);
          alignLine(lineStart, lastSpaceIndex, lastSpacePenX, fieldWidth, align);
          lineTop += lineHeight;
          for (int j = lastSpaceIndex; j < frames.getSize(); j++) {
            xs.set(j, xs.get(j) - shift);
            tops.set(j, tops.get(j) + lineHeight);
          }
          penX -= shift;
          lineStart = lastSpaceIndex;
          lineStarts.add(lineStart);
          lastSpaceIndex = -1;
        } else {
          maxLineWidth = Math.max(maxLineWidth, penX);
          alignLine(lineStart, frames.getSize(), penX, fieldWidth, align);
          penX = 0f;
          lineTop += lineHeight;
          lineStart = frames.getSize();
          lineStarts.add(lineStart);
        }
      }

      if (glyph.frame != null) {
        float gw = glyph.width * scale;
        float gh = glyph.height * scale;
        frames.add(glyph.frame);
        xs.add(penX + glyph.xOffset * scale);
        tops.add(lineTop + glyph.yOffset * scale);
        widths.add(gw);
        heights.add(gh);
      }
      penX += advance;
    }
    maxLineWidth = Math.max(maxLineWidth, penX);
    alignLine(lineStart, frames.getSize(), penX, fieldWidth > 0 ? fieldWidth : maxLineWidth, align);

    width = fieldWidth > 0 ? fieldWidth : maxLineWidth;
    height = lineTop + lineHeight;
    if (length == 0) {
      height = 0f;
    }
  }

  /**
   * Draws every glyph tinted with the batch's current color.
   *
   * @param batch The batch to draw through.
   * @param x The text block's left edge in world units.
   * @param y The text block's <em>top</em> edge in world units (y-down space).
   */
  public void draw(@NotNull FlixelBatch batch, float x, float y) {
    FlixelFrame[] items = frames.getItems();
    for (int i = 0, n = frames.getSize(); i < n; i++) {
      batch.draw(items[i], x + xs.get(i), y + tops.get(i), widths.get(i), heights.get(i));
    }
  }

  /**
   * Returns the laid-out text width in game pixels.
   */
  public float getWidth() {
    return width;
  }

  /**
   * Returns the laid-out text height in game pixels.
   */
  public float getHeight() {
    return height;
  }

  /** Shifts a finished line's glyphs right for center and right alignment. */
  private void alignLine(int from, int to, float lineWidth, float fieldWidth, int align) {
    if (align == FlixelAlign.LEFT || fieldWidth <= 0 || to <= from) {
      return;
    }
    float free = fieldWidth - lineWidth;
    if (free <= 0) {
      return;
    }
    float shift = (align == FlixelAlign.CENTER) ? free * 0.5f : free;
    for (int i = from; i < to; i++) {
      xs.set(i, xs.get(i) + shift);
    }
  }

  /** The advance of one space in game pixels, used when rewrapping at a space. */
  private static float spaceAdvanceAt(@NotNull FlixelFont font, float scale, float letterSpacing) {
    FlixelGlyph space = font.getGlyph(' ');
    return (space != null ? space.xAdvance * scale : 0f) + letterSpacing;
  }
}
