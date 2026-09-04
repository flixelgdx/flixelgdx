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
package org.flixelgdx.backend.html5.text;

import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.text.FlixelGlyphBitmap;
import org.flixelgdx.text.FlixelRasterizedFont;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.typedarrays.Int8Array;

import java.nio.ByteBuffer;

/**
 * One TrueType/OpenType font opened at a pixel size, rasterizing glyphs through the
 * stb_truetype WebAssembly module.
 *
 * <p>Each rasterized glyph is produced by the WASM module as an 8-bit per-pixel coverage bitmap
 * and expanded here to RGBA8888 white-with-alpha, matching the format the desktop backend
 * produces so the shared font baker works without any changes.
 *
 * <p>Two WASM heap pointers are kept alive for the lifetime of this object: {@code ctxPtr} (the
 * stb_truetype context) and {@code fontDataPtr} (the raw font bytes). stb_truetype holds an
 * internal pointer into the font data rather than copying it, so both must survive together and
 * are freed together in {@link #destroy()}.
 *
 * <p><b>Advance approximation:</b> the current WASM wrapper does not export stb's horizontal
 * metrics function ({@code stbtt_GetCodepointHMetrics}), so glyph advance is approximated as the
 * bitmap x-offset plus its width for visible glyphs, and as one third of the pixel height for
 * blank glyphs such as the space character. A future wrapper update that adds a proper
 * {@code stb_advance} export will remove this approximation.
 */
public class FlixelHtml5RasterizedFont implements FlixelRasterizedFont {

  protected final int ctxPtr;
  protected final int fontDataPtr;
  protected final float pixelHeight;
  protected final float ascent;
  protected final float descent;
  protected final float lineHeight;

  protected boolean destroyed;

  protected FlixelHtml5RasterizedFont(int ctxPtr, int fontDataPtr, float pixelHeight,
      float ascent, float descent, float lineHeight) {
    this.ctxPtr = ctxPtr;
    this.fontDataPtr = fontDataPtr;
    this.pixelHeight = pixelHeight;
    this.ascent = ascent;
    this.descent = descent;
    this.lineHeight = lineHeight;
  }

  @Override
  public @Nullable FlixelGlyphBitmap rasterize(int codepoint) {
    if (destroyed) {
      return null;
    }

    // metrics_out layout from stb_rasterize: [width, height, xOffset, yOffset]
    int metricsPtr = StbWasm.malloc(16);
    int bitmapPtr = StbWasm.stbRasterize(ctxPtr, codepoint, metricsPtr);

    int w = StbWasm.getInt(metricsPtr, 0);
    int h = StbWasm.getInt(metricsPtr, 1);
    int xoff = StbWasm.getInt(metricsPtr, 2);
    int yoff = StbWasm.getInt(metricsPtr, 3);
    StbWasm.free(metricsPtr);

    FlixelGlyphBitmap glyph = new FlixelGlyphBitmap();
    // Advance approximation: xoff + w for visible glyphs (assumes zero right-side bearing),
    // and one third of the pixel height for blank glyphs such as space.
    glyph.advance = (bitmapPtr != 0 && w > 0) ? xoff + w : pixelHeight * 0.333f;
    glyph.bearingX = xoff;
    // yoff from stb is negative above the baseline; bearingY is positive upward.
    glyph.bearingY = -yoff;

    if (bitmapPtr == 0 || w <= 0 || h <= 0) {
      return glyph;
    }

    // Obtain a live view of the coverage bitmap in WASM memory before the bitmap is freed, then
    // copy its bytes to a Java array. copyToJavaArray() creates an independent copy, so it is
    // safe to free the WASM pointer immediately after.
    Int8Array coverageView = StbWasm.heapView(bitmapPtr, w * h);
    byte[] coverage = coverageView.copyToJavaArray();
    StbWasm.stbFreeBitmap(bitmapPtr);

    // Expand 8-bit coverage to RGBA8888: white pixels with alpha equal to the coverage value,
    // matching the desktop format so the font baker can tint glyphs uniformly.
    FlixelImage image = new FlixelImage(w, h);
    ByteBuffer pixels = image.pixels();
    for (int i = 0, n = w * h; i < n; i++) {
      int o = i * 4;
      pixels.put(o, (byte) 0xFF);
      pixels.put(o + 1, (byte) 0xFF);
      pixels.put(o + 2, (byte) 0xFF);
      pixels.put(o + 3, coverage[i]);
    }
    glyph.image = image;

    return glyph;
  }

  @Override
  public void destroy() {
    if (!destroyed) {
      destroyed = true;
      StbWasm.stbFreeCtx(ctxPtr);
      StbWasm.free(fontDataPtr);
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

  public int getCtxPtr() {
    return ctxPtr;
  }

  public int getFontDataPtr() {
    return fontDataPtr;
  }

  public float getPixelHeight() {
    return pixelHeight;
  }

  public boolean isDestroyed() {
    return destroyed;
  }
}
