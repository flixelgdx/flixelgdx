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
import org.flixelgdx.collections.FlixelIntArray;
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
 *
 * <p>Alongside the drawn-glyph arrays (which skip spaces, newlines, and frameless glyphs, so a
 * glyph index does not line up with a character index), this class also keeps a per-character
 * caret table: {@link #getCharX(int)} and {@link #getCharLine(int)} give the caret position and
 * line for every character, including ones that draw nothing, so a UI text box can turn a click
 * into a caret index with {@link #getIndexAt(float, float)}.
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

  /**
   * Caret x position before each character, indexed by character index. Holds one extra entry
   * past the last character for the caret position after the final character.
   */
  @NotNull
  private final FlixelFloatArray charX = new FlixelFloatArray(64);

  /**
   * Line index of each character, indexed by character index. The trailing caret slot (see
   * {@link #charX}) takes the line index of the text's last line.
   */
  @NotNull
  private final FlixelIntArray charLine = new FlixelIntArray(64);

  /** Character index of the first character of each laid-out line, used for line lookups. */
  @NotNull
  private final FlixelIntArray lineStartChar = new FlixelIntArray(8);

  private float width;
  private float height;

  /** Distance between two consecutive line tops, in game pixels. */
  private float lineHeight;

  /** Number of laid-out lines; always at least {@code 1}, even for empty text. */
  private int lineCount;

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
    charX.clear();
    charLine.clear();
    lineStartChar.clear();

    lineHeight = font.getLineHeight() * scale;
    float penX = 0f;
    float lineTop = 0f;
    float maxLineWidth = 0f;
    int lineStart = 0;
    int lineStartCharIndex = 0;
    int currentLine = 0;
    int lastSpaceIndex = -1;
    float lastSpacePenX = 0f;
    int lastSpaceCharIndex = -1;
    int length = text.length();

    lineStartChar.add(0);
    for (int i = 0; i < length; i++) {
      char c = text.charAt(i);
      if (c == '\n') {
        // The newline itself belongs to the line it ends, so its caret entry is recorded
        // before the line advances.
        charX.add(penX);
        charLine.add(currentLine);
        maxLineWidth = Math.max(maxLineWidth, penX);
        alignLine(lineStart, frames.getSize(), lineStartCharIndex, i + 1, penX, fieldWidth, align);
        penX = 0f;
        lineTop += lineHeight;
        lineStart = frames.getSize();
        currentLine++;
        lineStartCharIndex = i + 1;
        lineStartChar.add(lineStartCharIndex);
        lastSpaceIndex = -1;
        lastSpaceCharIndex = -1;
        continue;
      }
      FlixelGlyph glyph = font.getGlyph(c);
      if (glyph == null) {
        charX.add(penX);
        charLine.add(currentLine);
        continue;
      }
      float advance = glyph.xAdvance * scale + letterSpacing;

      if (c == ' ') {
        lastSpaceIndex = frames.getSize();
        lastSpacePenX = penX;
        lastSpaceCharIndex = i;
      }

      // Word wrap: when this glyph would cross the field edge, move everything since the last
      // space down one line (or hard-break when a single word is wider than the field).
      if (wrap && fieldWidth > 0 && penX + advance > fieldWidth && penX > 0) {
        if (lastSpaceIndex >= 0 && lastSpaceIndex >= lineStart) {
          float shift = lastSpacePenX + spaceAdvanceAt(font, scale, letterSpacing);
          maxLineWidth = Math.max(maxLineWidth, lastSpacePenX);
          alignLine(lineStart, lastSpaceIndex, lineStartCharIndex, lastSpaceCharIndex + 1,
              lastSpacePenX, fieldWidth, align);
          lineTop += lineHeight;
          for (int j = lastSpaceIndex; j < frames.getSize(); j++) {
            xs.set(j, xs.get(j) - shift);
            tops.set(j, tops.get(j) + lineHeight);
          }
          int newLine = currentLine + 1;
          for (int k = lastSpaceCharIndex + 1; k < i; k++) {
            charX.set(k, charX.get(k) - shift);
            charLine.set(k, newLine);
          }
          penX -= shift;
          lineStart = lastSpaceIndex;
          currentLine = newLine;
          lineStartCharIndex = lastSpaceCharIndex + 1;
          lineStartChar.add(lineStartCharIndex);
          lastSpaceIndex = -1;
          lastSpaceCharIndex = -1;
        } else {
          maxLineWidth = Math.max(maxLineWidth, penX);
          alignLine(lineStart, frames.getSize(), lineStartCharIndex, i, penX, fieldWidth, align);
          penX = 0f;
          lineTop += lineHeight;
          lineStart = frames.getSize();
          currentLine++;
          lineStartCharIndex = i;
          lineStartChar.add(lineStartCharIndex);
        }
      }

      charX.add(penX);
      charLine.add(currentLine);

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
    float effectiveFieldWidth = fieldWidth > 0 ? fieldWidth : maxLineWidth;

    // The trailing caret slot is the position after the last character of the text.
    charX.add(penX);
    charLine.add(currentLine);
    if (length > 0) {
      alignLine(lineStart, frames.getSize(), lineStartCharIndex, length + 1, penX, effectiveFieldWidth, align);
    }

    width = effectiveFieldWidth;
    height = lineTop + lineHeight;
    lineCount = currentLine + 1;
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
   *
   * @return The total width of the laid-out text block in game pixels.
   */
  public float getWidth() {
    return width;
  }

  /**
   * Returns the laid-out text height in game pixels.
   *
   * @return The total height of the laid-out text block in game pixels.
   */
  public float getHeight() {
    return height;
  }

  /**
   * Returns the caret x position immediately before a character, in game pixels from the text
   * block's left edge.
   *
   * @param index The character index, from {@code 0} to the text length inclusive. The value at
   *     {@code length} is the caret position after the last character.
   * @return The caret x position in game pixels.
   */
  public float getCharX(int index) {
    return charX.get(index);
  }

  /**
   * Returns the line a character sits on.
   *
   * @param index The character index, from {@code 0} to the text length inclusive. The trailing
   *     entry at {@code length} reports the last line.
   * @return The zero-based line index.
   */
  public int getCharLine(int index) {
    return charLine.get(index);
  }

  /**
   * Returns the number of laid-out lines.
   *
   * @return The line count; always at least {@code 1}, even for empty text.
   */
  public int getLineCount() {
    return lineCount;
  }

  /**
   * Returns the distance between two consecutive line tops.
   *
   * @return The line height in game pixels.
   */
  public float getLineHeight() {
    return lineHeight;
  }

  /**
   * Returns a line's top edge, measured down from the text block's top edge.
   *
   * @param line The zero-based line index.
   * @return The line's top y position in game pixels.
   */
  public float getLineTop(int line) {
    return line * lineHeight;
  }

  /**
   * Returns the character index of the first character of a line.
   *
   * @param line The zero-based line index.
   * @return The first character index of {@code line}.
   */
  public int getLineStart(int line) {
    return lineStartChar.get(line);
  }

  /**
   * Returns the character index just past the last character of a line, excluding the newline
   * that ends it.
   *
   * @param line The zero-based line index.
   * @return The exclusive end character index of {@code line}.
   */
  public int getLineEnd(int line) {
    if (line + 1 < lineCount) {
      return lineStartChar.get(line + 1) - 1;
    }
    return charX.getSize() - 1;
  }

  /**
   * Maps a local point to the nearest caret index, for turning a click into a text-selection
   * position.
   *
   * <p>The point is in the same space as {@link #draw(FlixelBatch, float, float)}: game pixels
   * relative to the text block's top-left corner, before any sprite scale or rotation is applied.
   *
   * @param x The x position in game pixels from the text block's left edge.
   * @param y The y position in game pixels from the text block's top edge.
   * @return The caret index, from {@code 0} to the text length.
   */
  public int getIndexAt(float x, float y) {
    int line = lineHeight > 0f ? (int) Math.floor(y / lineHeight) : 0;
    if (line < 0) {
      line = 0;
    } else if (line > lineCount - 1) {
      line = lineCount - 1;
    }

    int from = getLineStart(line);
    int to = getLineEnd(line);
    int lo = from;
    int hi = to + 1;
    while (lo < hi) {
      int mid = (lo + hi) >>> 1;
      if (charX.get(mid) < x) {
        lo = mid + 1;
      } else {
        hi = mid;
      }
    }
    if (lo > to) {
      return to;
    }
    if (lo == from) {
      return from;
    }
    float leftX = charX.get(lo - 1);
    float rightX = charX.get(lo);
    return (x - leftX <= rightX - x) ? lo - 1 : lo;
  }

  /** Shifts a finished line's glyphs and caret positions right for center and right alignment. */
  private void alignLine(int fromGlyph, int toGlyph, int fromChar, int toChar, float lineWidth,
      float fieldWidth, int align) {
    if (align == FlixelAlign.LEFT || fieldWidth <= 0) {
      return;
    }
    float free = fieldWidth - lineWidth;
    if (free <= 0) {
      return;
    }
    float shift = (align == FlixelAlign.CENTER) ? free * 0.5f : free;
    for (int i = fromGlyph; i < toGlyph; i++) {
      xs.set(i, xs.get(i) + shift);
    }
    for (int i = fromChar; i < toChar; i++) {
      charX.set(i, charX.get(i) + shift);
    }
  }

  /** The advance of one space in game pixels, used when rewrapping at a space. */
  private static float spaceAdvanceAt(@NotNull FlixelFont font, float scale, float letterSpacing) {
    FlixelGlyph space = font.getGlyph(' ');
    return (space != null ? space.xAdvance * scale : 0f) + letterSpacing;
  }
}
