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

import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.text.FlixelGlyphBitmap;
import org.flixelgdx.text.FlixelRasterizedFont;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * One TrueType/OpenType font opened at a pixel size, rasterizing glyphs with stb_truetype.
 *
 * <p>Each rasterized glyph is expanded from stb's single-channel coverage bitmap into an RGBA
 * {@link FlixelImage} (white with per-pixel alpha), which the font baker blits into its atlas and
 * later tints. The metrics are converted from font units into pixels with the scale stb computes
 * for the requested pixel height.
 */
public class FlixelStbRasterizedFont implements FlixelRasterizedFont {

  /** The raw font file bytes, kept alive because stb references them for the font's lifetime. */
  private final ByteBuffer fontData;

  private final STBTTFontinfo info;

  private final float scale;
  private final float ascent;
  private final float descent;
  private final float lineHeight;

  private boolean destroyed;

  private FlixelStbRasterizedFont(ByteBuffer fontData, STBTTFontinfo info, float scale,
      float ascent, float descent, float lineHeight) {
    this.fontData = fontData;
    this.info = info;
    this.scale = scale;
    this.ascent = ascent;
    this.descent = descent;
    this.lineHeight = lineHeight;
  }

  /**
   * Opens font data at the given pixel height.
   *
   * @param data The raw {@code .ttf}/{@code .otf} bytes.
   * @param pixelHeight The target line height in pixels.
   * @return The opened font, or {@code null} when the data is not a supported font.
   */
  @Nullable
  public static FlixelStbRasterizedFont open(byte[] data, float pixelHeight) {
    ByteBuffer fontData = ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());
    fontData.put(data).flip();

    STBTTFontinfo info = STBTTFontinfo.malloc();
    if (!STBTruetype.stbtt_InitFont(info, fontData)) {
      info.free();
      return null;
    }

    float scale = STBTruetype.stbtt_ScaleForPixelHeight(info, pixelHeight);
    try (MemoryStack stack = MemoryStack.stackPush()) {
      IntBuffer ascentBuf = stack.mallocInt(1);
      IntBuffer descentBuf = stack.mallocInt(1);
      IntBuffer lineGapBuf = stack.mallocInt(1);
      STBTruetype.stbtt_GetFontVMetrics(info, ascentBuf, descentBuf, lineGapBuf);
      float ascent = ascentBuf.get(0) * scale;
      float descent = -descentBuf.get(0) * scale;
      float lineHeight = (ascentBuf.get(0) - descentBuf.get(0) + lineGapBuf.get(0)) * scale;
      return new FlixelStbRasterizedFont(fontData, info, scale, ascent, descent, lineHeight);
    }
  }

  @Override
  public float getAscent() {
    return ascent;
  }

  @Override
  public float getDescent() {
    return descent;
  }

  @Override
  public float getLineHeight() {
    return lineHeight;
  }

  @Nullable
  @Override
  public FlixelGlyphBitmap rasterize(int codepoint) {
    if (destroyed) {
      return null;
    }
    try (MemoryStack stack = MemoryStack.stackPush()) {
      IntBuffer advanceBuf = stack.mallocInt(1);
      IntBuffer leftBearingBuf = stack.mallocInt(1);
      STBTruetype.stbtt_GetCodepointHMetrics(info, codepoint, advanceBuf, leftBearingBuf);

      IntBuffer x0 = stack.mallocInt(1);
      IntBuffer y0 = stack.mallocInt(1);
      IntBuffer x1 = stack.mallocInt(1);
      IntBuffer y1 = stack.mallocInt(1);
      STBTruetype.stbtt_GetCodepointBitmapBox(info, codepoint, scale, scale, x0, y0, x1, y1);

      FlixelGlyphBitmap out = new FlixelGlyphBitmap();
      out.advance = advanceBuf.get(0) * scale;
      out.bearingX = x0.get(0);
      // y0 is negative above the baseline; bearingY is the distance up to the glyph's top edge.
      out.bearingY = -y0.get(0);

      int gw = x1.get(0) - x0.get(0);
      int gh = y1.get(0) - y0.get(0);
      if (gw <= 0 || gh <= 0) {
        // Blank glyph (space and friends): advance only, no pixels.
        return out;
      }

      IntBuffer bw = stack.mallocInt(1);
      IntBuffer bh = stack.mallocInt(1);
      IntBuffer bx = stack.mallocInt(1);
      IntBuffer by = stack.mallocInt(1);
      ByteBuffer coverage = STBTruetype.stbtt_GetCodepointBitmap(info, scale, scale, codepoint, bw, bh, bx, by);
      if (coverage == null) {
        return out;
      }
      try {
        int width = bw.get(0);
        int height = bh.get(0);
        FlixelImage image = new FlixelImage(width, height);
        ByteBuffer pixels = image.pixels();
        for (int i = 0, n = width * height; i < n; i++) {
          byte alpha = coverage.get(i);
          int o = i * 4;
          pixels.put(o, (byte) 0xFF);     // R
          pixels.put(o + 1, (byte) 0xFF); // G
          pixels.put(o + 2, (byte) 0xFF); // B
          pixels.put(o + 3, alpha);       // A: stb coverage
        }
        out.image = image;
      } finally {
        STBTruetype.stbtt_FreeBitmap(coverage, 0L);
      }
      return out;
    }
  }

  @Override
  public void destroy() {
    if (!destroyed) {
      destroyed = true;
      info.free();
    }
  }

  /** Returns the stb_truetype scale factor, i.e., pixels per font unit at the requested size. */
  public float getScale() {
    return scale;
  }

  public boolean isDestroyed() {
    return destroyed;
  }
}
