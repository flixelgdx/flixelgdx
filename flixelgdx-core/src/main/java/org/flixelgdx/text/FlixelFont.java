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

import org.flixelgdx.Flixel;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelIntMap;
import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelNoopTexture;
import org.flixelgdx.graphics.FlixelTexture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A ready-to-draw font: a set of {@link FlixelGlyph}s baked into one or more atlas textures.
 *
 * <p>A font comes from one of two places:
 *
 * <ul>
 *   <li><b>Scalable files</b> ({@code .ttf}, {@code .otf}) are baked at an exact pixel size
 *     through the platform's {@link FlixelFontRasterizer}; see
 *     {@link #bake(FlixelRasterizedFont, boolean)}.</li>
 *   <li><b>Bitmap fonts</b> (AngelCode {@code .fnt} text format plus a page image) are parsed
 *     directly; see {@link #fromFnt(String, FlixelImage)}. The framework's packaged default
 *     font uses this path, so text works even before any font is registered.</li>
 * </ul>
 *
 * <p>Fonts are cached and shared by {@link FlixelFontRegistry}; {@link FlixelText} looks them
 * up there. Metrics are stored in baked pixels; drawing code passes a scale factor to
 * {@link FlixelTextLayout} to map them into game pixels.
 */
public final class FlixelFont implements FlixelDestroyable {

  /** Codepoint drawn when a requested glyph is missing from the font. */
  private static final int FALLBACK_CODEPOINT = '?';

  /** First and last codepoints baked up front from scalable fonts (ASCII plus Latin-1). */
  private static final int BAKE_FIRST = 32;
  private static final int BAKE_LAST = 255;

  /** Fixed atlas width used while shelf-packing baked glyphs. */
  private static final int BAKE_PAGE_WIDTH = 512;

  /** Padding around every baked glyph so linear filtering never bleeds neighbors. */
  private static final int BAKE_PADDING = 1;

  @NotNull
  private final FlixelIntMap<FlixelGlyph> glyphs = new FlixelIntMap<>();

  @NotNull
  private final FlixelArray<FlixelTexture> pages = new FlixelArray<>(1);

  private float lineHeight;
  private float base;

  private FlixelFont() {}

  /**
   * Bakes a scalable font into an atlas at the size the rasterized font was opened with.
   *
   * <p>ASCII and Latin-1 codepoints (32 to 255) are baked up front. Codepoints outside that
   * range fall back to {@code '?'} when drawn; full dynamic Unicode baking can layer on later
   * without changing this API.
   *
   * @param raster The opened font to pull glyphs from; the caller destroys it afterward.
   * @param smooth {@code true} for linear filtering on the atlas texture.
   * @return The baked font; never {@code null}.
   */
  @NotNull
  public static FlixelFont bake(@NotNull FlixelRasterizedFont raster, boolean smooth) {
    FlixelFont font = new FlixelFont();
    font.lineHeight = raster.getLineHeight();
    font.base = raster.getAscent();

    // First pass: rasterize everything so total atlas height is known before allocating.
    FlixelArray<FlixelGlyphBitmap> bitmaps = new FlixelArray<>(BAKE_LAST - BAKE_FIRST + 1);
    int penX = BAKE_PADDING;
    int penY = BAKE_PADDING;
    int shelfHeight = 0;
    for (int cp = BAKE_FIRST; cp <= BAKE_LAST; cp++) {
      FlixelGlyphBitmap bitmap = raster.rasterize(cp);
      bitmaps.add(bitmap);
      if (bitmap == null || bitmap.image == null) {
        continue;
      }
      int w = bitmap.image.width() + BAKE_PADDING;
      int h = bitmap.image.height() + BAKE_PADDING;
      if (penX + w > BAKE_PAGE_WIDTH) {
        penX = BAKE_PADDING;
        penY += shelfHeight;
        shelfHeight = 0;
      }
      penX += w;
      shelfHeight = Math.max(shelfHeight, h);
    }
    int pageHeight = nextPowerOfTwo(penY + shelfHeight + BAKE_PADDING);

    // Second pass: blit into the atlas image and record glyph metrics.
    FlixelImage atlas = new FlixelImage(BAKE_PAGE_WIDTH, pageHeight);
    penX = BAKE_PADDING;
    penY = BAKE_PADDING;
    shelfHeight = 0;
    for (int cp = BAKE_FIRST; cp <= BAKE_LAST; cp++) {
      FlixelGlyphBitmap bitmap = bitmaps.get(cp - BAKE_FIRST);
      if (bitmap == null) {
        continue;
      }
      FlixelGlyph glyph = new FlixelGlyph();
      glyph.xAdvance = bitmap.advance;
      glyph.xOffset = bitmap.bearingX;
      glyph.page = 0;
      if (bitmap.image != null) {
        int w = bitmap.image.width();
        int h = bitmap.image.height();
        if (penX + w + BAKE_PADDING > BAKE_PAGE_WIDTH) {
          penX = BAKE_PADDING;
          penY += shelfHeight;
          shelfHeight = 0;
        }
        atlas.draw(bitmap.image, penX, penY);
        glyph.x = penX;
        glyph.y = penY;
        glyph.width = w;
        glyph.height = h;
        // bearingY measures baseline-to-top; the line's top sits ascent above the baseline.
        glyph.yOffset = font.base - bitmap.bearingY;
        penX += w + BAKE_PADDING;
        shelfHeight = Math.max(shelfHeight, h + BAKE_PADDING);
      }
      font.glyphs.put(cp, glyph);
    }

    FlixelTexture page = Flixel.graphics.createTexture(atlas);
    page.setSmooth(smooth);
    font.pages.add(page);
    font.createGlyphFrames();
    return font;
  }

  /**
   * Parses an AngelCode {@code .fnt} file (text format) into a font.
   *
   * @param fntText The {@code .fnt} file contents.
   * @param pageImage The decoded page image, or {@code null} when no decoder is available (the
   *     font then measures correctly but draws nothing, which keeps headless sessions safe).
   * @return The parsed font; never {@code null}.
   */
  @NotNull
  public static FlixelFont fromFnt(@NotNull String fntText, @Nullable FlixelImage pageImage) {
    FlixelFont font = new FlixelFont();
    int scaleW = 0;
    int scaleH = 0;
    for (String line : fntText.split("\n")) {
      line = line.trim();
      if (line.startsWith("common ")) {
        font.lineHeight = intAttr(line, "lineHeight");
        font.base = intAttr(line, "base");
        scaleW = intAttr(line, "scaleW");
        scaleH = intAttr(line, "scaleH");
      } else if (line.startsWith("char ")) {
        FlixelGlyph glyph = new FlixelGlyph();
        int id = intAttr(line, "id");
        glyph.x = intAttr(line, "x");
        glyph.y = intAttr(line, "y");
        glyph.width = intAttr(line, "width");
        glyph.height = intAttr(line, "height");
        glyph.xOffset = intAttr(line, "xoffset");
        glyph.yOffset = intAttr(line, "yoffset");
        glyph.xAdvance = intAttr(line, "xadvance");
        glyph.page = intAttr(line, "page");
        font.glyphs.put(id, glyph);
      }
    }
    FlixelTexture page;
    if (pageImage != null) {
      page = Flixel.graphics.createTexture(pageImage);
    } else {
      page = new FlixelNoopTexture(Math.max(1, scaleW), Math.max(1, scaleH));
    }
    font.pages.add(page);
    font.createGlyphFrames();
    return font;
  }

  /**
   * Looks a glyph up, falling back to {@code '?'} for codepoints the font does not cover.
   *
   * @param codepoint The Unicode codepoint to look up.
   * @return The glyph, or {@code null} when even the fallback is missing.
   */
  @Nullable
  public FlixelGlyph getGlyph(int codepoint) {
    FlixelGlyph glyph = glyphs.get(codepoint);
    if (glyph == null) {
      glyph = glyphs.get(FALLBACK_CODEPOINT);
    }
    return glyph;
  }

  /**
   * Switches every atlas page between smooth (linear) and crisp (nearest) sampling.
   *
   * @param smooth {@code true} for linear filtering.
   */
  public void setSmooth(boolean smooth) {
    for (int i = 0; i < pages.getSize(); i++) {
      pages.get(i).setSmooth(smooth);
    }
  }

  @Override
  public void destroy() {
    for (int i = 0; i < pages.getSize(); i++) {
      pages.get(i).destroy();
    }
    pages.clear();
    glyphs.clear();
  }

  /**
   * Returns the vertical distance between two consecutive line tops, in baked pixels.
   *
   * @return The line height in baked pixels.
   */
  public float getLineHeight() {
    return lineHeight;
  }

  /**
   * Returns the distance from a line's top to its baseline, in baked pixels.
   *
   * @return The baseline offset from the top of a line, in baked pixels.
   */
  public float getBase() {
    return base;
  }

  /** Builds the reusable draw frame for every glyph once the pages exist. */
  private void createGlyphFrames() {
    FlixelIntMap.Values<FlixelGlyph> values = glyphs.values();
    while (values.hasNext()) {
      FlixelGlyph glyph = values.next();
      if (glyph.width > 0 && glyph.height > 0 && glyph.page < pages.getSize()) {
        glyph.frame = new FlixelFrame(pages.get(glyph.page), glyph.x, glyph.y, glyph.width, glyph.height);
      }
    }
  }

  private static int nextPowerOfTwo(int value) {
    int result = 1;
    while (result < value) {
      result <<= 1;
    }
    return result;
  }

  /** Reads one integer attribute such as {@code x=12} out of a {@code .fnt} line. */
  private static int intAttr(@NotNull String line, @NotNull String name) {
    int at = line.indexOf(name + "=");
    if (at < 0) {
      return 0;
    }
    int start = at + name.length() + 1;
    int end = start;
    while (end < line.length() && line.charAt(end) != ' ') {
      end++;
    }
    try {
      return Integer.parseInt(line.substring(start, end));
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
