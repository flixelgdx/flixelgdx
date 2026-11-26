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

import org.flixelgdx.util.FlixelAlign;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlixelTextLayoutTest {

  private static final float LETTER_ADVANCE = 10f;

  private static final float SPACE_ADVANCE = 6f;

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
    assertEquals(10f, layout.getCharX(1));
    assertEquals(0, layout.getCharLine(1));
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
    layout.set(buildFont(), "abcdef", 1f, 25f, true, FlixelAlign.LEFT, 0f);

    assertEquals(3, layout.getLineCount());
    assertEquals(0, layout.getLineStart(0));
    assertEquals(2, layout.getLineStart(1));
    assertEquals(4, layout.getLineStart(2));

    assertEquals(layout.getLineStart(1), layout.getLineEnd(0));
    assertEquals(layout.getLineStart(2), layout.getLineEnd(1));
    assertEquals(6, layout.getLineEnd(2));

    assertEquals(0f, layout.getCharX(2));
    assertEquals(1, layout.getCharLine(2));
    assertEquals(0f, layout.getCharX(4));
    assertEquals(2, layout.getCharLine(4));

    assertEquals(25f, layout.getWidth());
    assertEquals(3f * LINE_HEIGHT, layout.getHeight());
  }

  @Test
  void getIndexAtPastAHardBreakLineEndReturnsTheNextLinesStart() {
    FlixelTextLayout layout = new FlixelTextLayout();
    layout.set(buildFont(), "abcdef", 1f, 25f, true, FlixelAlign.LEFT, 0f);

    assertEquals(layout.getLineEnd(0), layout.getIndexAt(1000f, 0f));
    assertEquals(2, layout.getIndexAt(1000f, 0f));
  }

  @Test
  void centerAlignmentShiftsCharX() {
    FlixelTextLayout layout = new FlixelTextLayout();
    layout.set(buildFont(), "ab", 1f, 50f, false, FlixelAlign.CENTER, 0f);

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

    assertEquals(layout.getWidth(), layout.getCharX(3));
    assertEquals(layout.getLineCount() - 1, layout.getCharLine(3));
  }

  @Test
  void getIndexAtSnapsToTheNearerCharEdgeWithinALine() {
    FlixelTextLayout layout = new FlixelTextLayout();
    layout.set(buildFont(), "ab cd", 1f, 30f, true, FlixelAlign.LEFT, 0f);

    assertEquals(0, layout.getIndexAt(5f, 0f));
    assertEquals(1, layout.getIndexAt(9f, 0f));
    assertEquals(2, layout.getIndexAt(25f, 0f));
  }

  @Test
  void getIndexAtClampsAtLineStartAndEnd() {
    FlixelTextLayout layout = new FlixelTextLayout();
    layout.set(buildFont(), "ab cd", 1f, 30f, true, FlixelAlign.LEFT, 0f);

    assertEquals(0, layout.getIndexAt(-100f, 0f));
    assertEquals(5, layout.getIndexAt(1000f, 1000f));
    assertEquals(0, layout.getIndexAt(-100f, -100f));
  }

  @Test
  void getIndexAtPicksTheRequestedLineFromY() {
    FlixelTextLayout layout = new FlixelTextLayout();
    layout.set(buildFont(), "ab cd", 1f, 30f, true, FlixelAlign.LEFT, 0f);

    assertEquals(3, layout.getIndexAt(0f, LINE_HEIGHT + 1f));
  }

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
