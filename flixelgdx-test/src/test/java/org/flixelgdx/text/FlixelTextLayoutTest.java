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

import org.flixelgdx.graphics.FlixelNoopTexture;
import org.flixelgdx.util.FlixelAlign;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link FlixelTextLayout}, covering both the drawn-glyph arrays and the
 * per-character caret table used for click-to-index mapping.
 */
class FlixelTextLayoutTest {

  /** Horizontal advance of every letter glyph in the test font, in baked pixels. */
  private static final float LETTER_ADVANCE = 10f;

  /** Horizontal advance of the space glyph in the test font, in baked pixels. */
  private static final float SPACE_ADVANCE = 6f;

  /** Line height of the test font, in baked pixels. */
  private static final float LINE_HEIGHT = 12f;

  @Test
  void singleLinePositions() {
    FlixelTextLayout layout = new FlixelTextLayout();
    layout.set(buildFont(), "abc", 1f, 0f, false, FlixelAlign.LEFT, 0f);

    assertEquals(0f, layout.getCharX(0));
    assertEquals(10f, layout.getCharX(1));
    assertEquals(20f, layout.getCharX(2));
    assertEquals(30f, layout.getCharX(3));
    assertEquals(0, layout.getCharLine(0));
    assertEquals(0, layout.getCharLine(1));
    assertEquals(0, layout.getCharLine(2));
    assertEquals(0, layout.getCharLine(3));
    assertEquals(1, layout.getLineCount());
    assertEquals(LINE_HEIGHT, layout.getLineHeight());
    assertEquals(30f, layout.getWidth());
    assertEquals(LINE_HEIGHT, layout.getHeight());
  }

  @Test
  void spacesGetRealCaretEntriesButNoFrame() {
    FlixelTextLayout layout = new FlixelTextLayout();
    layout.set(buildFont(), "a b", 1f, 0f, false, FlixelAlign.LEFT, 0f);

    assertEquals(0f, layout.getCharX(0));
    assertEquals(10f, layout.getCharX(1));
    assertEquals(16f, layout.getCharX(2));
    assertEquals(26f, layout.getCharX(3));
    assertEquals(26f, layout.getWidth());
  }

  @Test
  void missingGlyphContributesNoAdvance() {
    FlixelTextLayout layout = new FlixelTextLayout();
    // 'z' is not registered in the test font and there is no '?' fallback, so it resolves to
    // a missing glyph.
    layout.set(buildFont(), "az", 1f, 0f, false, FlixelAlign.LEFT, 0f);

    assertEquals(0f, layout.getCharX(0));
    assertEquals(10f, layout.getCharX(1));
    assertEquals(10f, layout.getCharX(2));
    assertEquals(0, layout.getCharLine(1));
    assertEquals(10f, layout.getWidth());
  }

  @Test
  void newlineEndsThePreviousLine() {
    FlixelTextLayout layout = new FlixelTextLayout();
    layout.set(buildFont(), "a\nb", 1f, 0f, false, FlixelAlign.LEFT, 0f);

    assertEquals(0f, layout.getCharX(0));
    assertEquals(0, layout.getCharLine(0));
    // The newline belongs to the line it ends.
    assertEquals(10f, layout.getCharX(1));
    assertEquals(0, layout.getCharLine(1));
    // 'b' starts the new line at x = 0.
    assertEquals(0f, layout.getCharX(2));
    assertEquals(1, layout.getCharLine(2));
    assertEquals(10f, layout.getCharX(3));
    assertEquals(1, layout.getCharLine(3));

    assertEquals(2, layout.getLineCount());
    assertEquals(0, layout.getLineStart(0));
    assertEquals(1, layout.getLineEnd(0));
    assertEquals(2, layout.getLineStart(1));
    assertEquals(3, layout.getLineEnd(1));
    assertEquals(2f * LINE_HEIGHT, layout.getHeight());
  }

  @Test
  void wordWrapMovesTrailingWordToNextLine() {
    FlixelTextLayout layout = new FlixelTextLayout();
    // "ab cd" with field width 30: "ab " fits, but adding 'c' would overflow, so "cd" moves down.
    layout.set(buildFont(), "ab cd", 1f, 30f, true, FlixelAlign.LEFT, 0f);

    assertEquals(0f, layout.getCharX(0));
    assertEquals(10f, layout.getCharX(1));
    assertEquals(20f, layout.getCharX(2));
    assertEquals(0, layout.getCharLine(0));
    assertEquals(0, layout.getCharLine(1));
    assertEquals(0, layout.getCharLine(2));

    assertEquals(0f, layout.getCharX(3));
    assertEquals(10f, layout.getCharX(4));
    assertEquals(20f, layout.getCharX(5));
    assertEquals(1, layout.getCharLine(3));
    assertEquals(1, layout.getCharLine(4));
    assertEquals(1, layout.getCharLine(5));

    assertEquals(2, layout.getLineCount());
    assertEquals(0, layout.getLineStart(0));
    assertEquals(2, layout.getLineEnd(0));
    assertEquals(3, layout.getLineStart(1));
    assertEquals(5, layout.getLineEnd(1));
    assertEquals(30f, layout.getWidth());
    assertEquals(2f * LINE_HEIGHT, layout.getHeight());
  }

  @Test
  void hardBreakSplitsAWordWiderThanTheField() {
    FlixelTextLayout layout = new FlixelTextLayout();
    // No spaces anywhere, so every overflow is a hard break.
    layout.set(buildFont(), "abcdef", 1f, 25f, true, FlixelAlign.LEFT, 0f);

    assertEquals(3, layout.getLineCount());
    assertEquals(0, layout.getLineStart(0));
    assertEquals(2, layout.getLineStart(1));
    assertEquals(4, layout.getLineStart(2));

    assertEquals(0f, layout.getCharX(2));
    assertEquals(1, layout.getCharLine(2));
    assertEquals(0f, layout.getCharX(4));
    assertEquals(2, layout.getCharLine(4));

    assertEquals(25f, layout.getWidth());
    assertEquals(3f * LINE_HEIGHT, layout.getHeight());
  }

  @Test
  void centerAlignmentShiftsCharX() {
    FlixelTextLayout layout = new FlixelTextLayout();
    layout.set(buildFont(), "ab", 1f, 50f, false, FlixelAlign.CENTER, 0f);

    // Natural line width is 20; free space is 30, so center shifts everything by 15.
    assertEquals(15f, layout.getCharX(0));
    assertEquals(25f, layout.getCharX(1));
    assertEquals(35f, layout.getCharX(2));
  }

  @Test
  void rightAlignmentShiftsCharX() {
    FlixelTextLayout layout = new FlixelTextLayout();
    layout.set(buildFont(), "ab", 1f, 50f, false, FlixelAlign.RIGHT, 0f);

    assertEquals(30f, layout.getCharX(0));
    assertEquals(40f, layout.getCharX(1));
    assertEquals(50f, layout.getCharX(2));
  }

  @Test
  void emptyTextHasOneLineAndASingleCaretSlot() {
    FlixelTextLayout layout = new FlixelTextLayout();
    layout.set(buildFont(), "", 1f, 0f, false, FlixelAlign.LEFT, 0f);

    assertEquals(1, layout.getLineCount());
    assertEquals(0f, layout.getCharX(0));
    assertEquals(0, layout.getCharLine(0));
    assertEquals(0f, layout.getWidth());
    assertEquals(0f, layout.getHeight());
  }

  @Test
  void emptyTextIgnoresAlignment() {
    FlixelTextLayout layout = new FlixelTextLayout();
    layout.set(buildFont(), "", 1f, 50f, false, FlixelAlign.CENTER, 0f);

    assertEquals(0f, layout.getCharX(0));
  }

  @Test
  void trailingCaretSlotSitsAfterTheLastCharacter() {
    FlixelTextLayout layout = new FlixelTextLayout();
    layout.set(buildFont(), "abc", 1f, 0f, false, FlixelAlign.LEFT, 0f);

    // Index 3 (text.length()) is the caret slot after the last character.
    assertEquals(layout.getWidth(), layout.getCharX(3));
    assertEquals(layout.getLineCount() - 1, layout.getCharLine(3));
  }

  @Test
  void getIndexAtSnapsToTheNearerCharEdgeWithinALine() {
    FlixelTextLayout layout = new FlixelTextLayout();
    layout.set(buildFont(), "ab cd", 1f, 30f, true, FlixelAlign.LEFT, 0f);

    // Exactly between charX(0)=0 and charX(1)=10; ties snap to the left edge.
    assertEquals(0, layout.getIndexAt(5f, 0f));
    // Closer to charX(1)=10 than charX(0)=0.
    assertEquals(1, layout.getIndexAt(9f, 0f));
    // End of the first line (the space caret slot, index 2, at x = 20).
    assertEquals(2, layout.getIndexAt(25f, 0f));
  }

  @Test
  void getIndexAtClampsAtLineStartAndEnd() {
    FlixelTextLayout layout = new FlixelTextLayout();
    layout.set(buildFont(), "ab cd", 1f, 30f, true, FlixelAlign.LEFT, 0f);

    // Far left of the first line snaps to its start.
    assertEquals(0, layout.getIndexAt(-100f, 0f));
    // Far right of the second line (y beyond the layout) snaps to the last caret index.
    assertEquals(5, layout.getIndexAt(1000f, 1000f));
    // Negative y clamps to the first line.
    assertEquals(0, layout.getIndexAt(-100f, -100f));
  }

  @Test
  void getIndexAtPicksTheRequestedLineFromY() {
    FlixelTextLayout layout = new FlixelTextLayout();
    layout.set(buildFont(), "ab cd", 1f, 30f, true, FlixelAlign.LEFT, 0f);

    // y = LINE_HEIGHT + 1 lands on the second line, whose chars start at index 3.
    assertEquals(3, layout.getIndexAt(0f, LINE_HEIGHT + 1f));
  }

  /**
   * Builds a small test font by hand: fixed-advance letter glyphs with real frames on a
   * {@link FlixelNoopTexture}, plus a frameless space glyph. There is no '?' fallback glyph, so
   * looking up an unregistered character (such as 'z') resolves to a missing glyph, matching how
   * {@link FlixelFont#getGlyph(int)} behaves for uncovered fonts.
   */
  private static FlixelFont buildFont() {
    String fnt = "common lineHeight=" + (int) LINE_HEIGHT + " base=10 scaleW=64 scaleH=64\n"
        + "char id=32 x=0 y=0 width=0 height=0 xoffset=0 yoffset=0 xadvance=" + (int) SPACE_ADVANCE + " page=0\n"
        + "char id=97 x=0 y=0 width=8 height=8 xoffset=0 yoffset=0 xadvance=" + (int) LETTER_ADVANCE + " page=0\n"
        + "char id=98 x=8 y=0 width=8 height=8 xoffset=0 yoffset=0 xadvance=" + (int) LETTER_ADVANCE + " page=0\n"
        + "char id=99 x=16 y=0 width=8 height=8 xoffset=0 yoffset=0 xadvance=" + (int) LETTER_ADVANCE + " page=0\n"
        + "char id=100 x=24 y=0 width=8 height=8 xoffset=0 yoffset=0 xadvance=" + (int) LETTER_ADVANCE + " page=0\n"
        + "char id=101 x=32 y=0 width=8 height=8 xoffset=0 yoffset=0 xadvance=" + (int) LETTER_ADVANCE + " page=0\n"
        + "char id=102 x=40 y=0 width=8 height=8 xoffset=0 yoffset=0 xadvance=" + (int) LETTER_ADVANCE + " page=0\n";
    return FlixelFont.fromFnt(fnt, null);
  }
}
