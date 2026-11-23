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

import org.flixelgdx.collections.FlixelIntMap;
import org.flixelgdx.graphics.FlixelImage;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for glyphs that {@link FlixelFont} bakes on demand, outside the up-front Latin-1 range.
 */
class FlixelFontDynamicBakeTest {

  /** A codepoint the fake font pretends it cannot produce. */
  private static final int UNPRODUCIBLE = 0x2603;

  @Test
  void latinOneIsBakedUpFront() {
    FakeRaster raster = new FakeRaster(10, 12);
    FlixelFont font = FlixelFont.bake(raster, false);
    int before = raster.calls;
    assertNotNull(font.getGlyph('A'));
    assertNotNull(font.getGlyph(0xF1)); // ñ
    assertEquals(before, raster.calls);
  }

  @Test
  void newCharacterIsBakedOnceOnFirstUse() {
    FakeRaster raster = new FakeRaster(10, 12);
    FlixelFont font = FlixelFont.bake(raster, false);
    int before = raster.calls;

    FlixelGlyph sun = font.getGlyph(0x65E5); // 日
    assertNotNull(sun);
    assertEquals(before + 1, raster.calls);
    assertEquals(10, sun.width);
    assertEquals(12, sun.height);
    assertEquals(11f, sun.xAdvance);
    assertNotNull(sun.frame);
    assertTrue(sun.page > 0, "Glyphs baked on demand go on a page of their own.");

    assertSame(sun, font.getGlyph(0x65E5));
    assertEquals(before + 1, raster.calls);
  }

  @Test
  void unproducibleCharacterFallsBackAndIsOnlyTriedOnce() {
    FakeRaster raster = new FakeRaster(10, 12);
    FlixelFont font = FlixelFont.bake(raster, false);
    FlixelGlyph question = font.getGlyph('?');
    int before = raster.calls;

    assertSame(question, font.getGlyph(UNPRODUCIBLE));
    assertSame(question, font.getGlyph(UNPRODUCIBLE));
    assertEquals(before + 1, raster.calls);
  }

  @Test
  void glyphsFillPagesWithoutOverlapping() {
    FakeRaster raster = new FakeRaster(100, 100);
    FlixelFont font = FlixelFont.bake(raster, false);
    FlixelIntMap<FlixelGlyph> seen = new FlixelIntMap<>();
    int maxPage = 0;
    for (int cp = 0x4E00; cp < 0x4E00 + 60; cp++) {
      FlixelGlyph glyph = font.getGlyph(cp);
      assertNotNull(glyph);
      assertTrue(glyph.x >= 0 && glyph.y >= 0);
      assertTrue(glyph.x + glyph.width <= 512 && glyph.y + glyph.height <= 512, "Glyph must fit inside its page.");
      seen.put(cp, glyph);
      maxPage = Math.max(maxPage, glyph.page);
    }
    // A 512 page holds 5 by 5 glyphs of 100 pixels (plus padding), so 60 glyphs need three dynamic pages.
    assertTrue(maxPage >= 3, "Expected new pages once one fills up, got last page " + maxPage);

    for (int a = 0x4E00; a < 0x4E00 + 60; a++) {
      for (int b = a + 1; b < 0x4E00 + 60; b++) {
        FlixelGlyph ga = seen.get(a);
        FlixelGlyph gb = seen.get(b);
        if (ga.page != gb.page) {
          continue;
        }
        boolean apart = ga.x + ga.width <= gb.x || gb.x + gb.width <= ga.x
            || ga.y + ga.height <= gb.y || gb.y + gb.height <= ga.y;
        assertTrue(apart, "Glyphs " + a + " and " + b + " overlap.");
      }
    }
  }

  @Test
  void glyphBiggerThanAPageGetsALargerPage() {
    FakeRaster raster = new FakeRaster(10, 12);
    FlixelFont font = FlixelFont.bake(raster, false);
    raster.width = 700;
    raster.height = 700;
    FlixelGlyph huge = font.getGlyph(0x9F8D);
    assertNotNull(huge);
    assertEquals(700, huge.width);
    assertNotNull(huge.frame);
    assertEquals(1024, huge.frame.getTexture().getWidth());

    raster.width = 10;
    raster.height = 12;
    FlixelGlyph small = font.getGlyph(0x4E00);
    assertNotNull(small);
    assertNotSame(huge, small);
  }

  @Test
  void destroyClosesTheRasterizedFont() {
    FakeRaster raster = new FakeRaster(10, 12);
    FlixelFont font = FlixelFont.bake(raster, false);
    assertEquals(0, raster.destroyCount);
    font.destroy();
    assertEquals(1, raster.destroyCount);
  }

  @Test
  void bitmapFontsStillFallBackToQuestionMark() {
    FlixelFont font = FlixelFont.fromFnt("""
        common lineHeight=12 base=10 scaleW=64 scaleH=64
        char id=63 x=0 y=0 width=5 height=8 xoffset=0 yoffset=2 xadvance=6 page=0
        """, null);
    assertSame(font.getGlyph('?'), font.getGlyph(0x65E5));
  }

  /** A pretend font where every character is a solid rectangle of the current size. */
  private static final class FakeRaster implements FlixelRasterizedFont {

    int width;
    int height;
    int calls;
    int destroyCount;

    FakeRaster(int width, int height) {
      this.width = width;
      this.height = height;
    }

    @Override
    public float getAscent() {
      return 10f;
    }

    @Override
    public float getDescent() {
      return 2f;
    }

    @Override
    public float getLineHeight() {
      return 12f;
    }

    @Override
    public @Nullable FlixelGlyphBitmap rasterize(int codepoint) {
      calls++;
      if (codepoint == UNPRODUCIBLE) {
        return null;
      }
      FlixelGlyphBitmap bitmap = new FlixelGlyphBitmap();
      bitmap.advance = width + 1f;
      bitmap.bearingY = 10f;
      if (codepoint != ' ') {
        bitmap.image = new FlixelImage(width, height);
      }
      return bitmap;
    }

    @Override
    public void destroy() {
      destroyCount++;
    }
  }
}
