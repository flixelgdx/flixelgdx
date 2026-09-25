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
package org.flixelgdx.backend.android.text;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.text.FlixelGlyphBitmap;
import org.flixelgdx.text.FlixelRasterizedFont;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * One font opened at a pixel size using Android's native Typeface and Paint APIs.
 *
 * <p>Metrics are calibrated so that ascent + descent equals the requested pixel height, matching
 * the convention the desktop rasterizer uses. This means text rendered at the same pixel size
 * looks the same height across platforms, up to the rasterizer's own hinting differences.
 *
 * <p>A single reusable ALPHA_8 scratch {@link Bitmap} is kept for the lifetime of this font.
 * It grows when a glyph is larger than the current scratch dimensions, but never shrinks.
 * Each call to {@link #rasterize(int)} erases the bitmap, draws the glyph, copies the
 * coverage bytes, and builds the RGBA {@link FlixelImage} the font baker expects.
 *
 * @see FlixelAndroidFontRasterizer
 */
class FlixelAndroidRasterizedFont implements FlixelRasterizedFont {

  private final float ascent;
  private final float descent;
  private final float lineHeight;
  private int scratchW;
  private int scratchH;
  private final File cacheFile;
  private final Paint paint;
  private Bitmap scratch;
  private Canvas scratchCanvas;
  private final char[] charBuf;
  private final float[] widthBuf;
  private final Rect bounds;
  private ByteBuffer coverageBuf;
  private final boolean ownsFile;
  private boolean destroyed;

  FlixelAndroidRasterizedFont(Typeface typeface, File cacheFile, boolean ownsFile,
      float pixelHeight) {
    this.cacheFile = cacheFile;
    this.ownsFile = ownsFile;

    // Build and calibrate the paint.
    // Android's ascent is negative (above baseline) and descent is positive (below baseline).
    // We want ascent - (-ascent) = descent - ascent = pixelHeight after calibration.
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setSubpixelText(true);
    paint.setTypeface(typeface);
    paint.setColor(Color.WHITE);
    paint.setTextSize(pixelHeight);

    Paint.FontMetrics fm = paint.getFontMetrics();
    // rawSpan = descent - ascent (ascent < 0, so span > 0).
    float rawSpan = fm.descent - fm.ascent;
    if (rawSpan > 0f) {
      paint.setTextSize(pixelHeight * pixelHeight / rawSpan);
      fm = paint.getFontMetrics();
    }

    // Convert Android sign conventions to core conventions (both positive).
    this.ascent = -fm.ascent;
    this.descent = fm.descent;
    this.lineHeight = fm.descent - fm.ascent + Math.max(0f, fm.leading);

    // Allocate the scratch bitmap and its canvas.
    scratchW = 64;
    scratchH = 64;
    scratch = Bitmap.createBitmap(scratchW, scratchH, Bitmap.Config.ALPHA_8);
    scratchCanvas = new Canvas(scratch);

    charBuf = new char[2];
    widthBuf = new float[2];
    bounds = new Rect();
    coverageBuf = ByteBuffer.allocate(scratchW * scratchH).order(ByteOrder.nativeOrder());
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

    int charCount = Character.toChars(codepoint, charBuf, 0);

    // Horizontal advance: sum both slots because a surrogate pair has the advance split
    // across slots (second slot is typically 0, but summing is correct in all cases).
    paint.getTextWidths(charBuf, 0, charCount, widthBuf);
    float advance = widthBuf[0];
    if (charCount == 2) {
      advance += widthBuf[1];
    }

    // Tight pixel bounding box relative to the baseline origin.
    paint.getTextBounds(charBuf, 0, charCount, bounds);
    int gw = bounds.width();
    int gh = bounds.height();

    FlixelGlyphBitmap out = new FlixelGlyphBitmap();
    out.advance = advance;
    // bounds.left is the horizontal offset from the pen to the left edge of the box.
    out.bearingX = bounds.left;
    // bounds.top is negative for text above the baseline; negate to get a positive distance.
    out.bearingY = -bounds.top;

    if (gw <= 0 || gh <= 0) {
      // Blank glyph such as space: return advance and metrics, no pixels.
      return out;
    }

    // Grow the scratch bitmap when the glyph does not fit.
    if (gw > scratchW || gh > scratchH) {
      int newW = Math.max(gw, scratchW);
      int newH = Math.max(gh, scratchH);
      scratch.recycle();
      scratchW = newW;
      scratchH = newH;
      scratch = Bitmap.createBitmap(scratchW, scratchH, Bitmap.Config.ALPHA_8);
      scratchCanvas = new Canvas(scratch);
      coverageBuf = ByteBuffer.allocate(scratchW * scratchH).order(ByteOrder.nativeOrder());
    }

    // Clear and draw. We position the text so that the bounding box top-left lands at (0, 0):
    // the pen baseline is at y = -bounds.top and the pen x is at -bounds.left.
    scratch.eraseColor(Color.TRANSPARENT);
    scratchCanvas.drawText(charBuf, 0, charCount, -bounds.left, -bounds.top, paint);

    // Copy the ALPHA_8 coverage pixels. Each row in the bitmap is scratchW bytes wide.
    coverageBuf.clear();
    scratch.copyPixelsToBuffer(coverageBuf);
    coverageBuf.rewind();

    // Build the RGBA image: white with the ALPHA_8 coverage as alpha, matching desktop format.
    FlixelImage image = new FlixelImage(gw, gh);
    ByteBuffer pixels = image.getPixels();
    for (int row = 0; row < gh; row++) {
      for (int col = 0; col < gw; col++) {
        byte alpha = coverageBuf.get(row * scratchW + col);
        int o = (row * gw + col) * 4;
        pixels.put(o, (byte) 0xFF);
        pixels.put(o + 1, (byte) 0xFF);
        pixels.put(o + 2, (byte) 0xFF);
        pixels.put(o + 3, alpha);
      }
    }
    out.image = image;
    return out;
  }

  @Override
  public void destroy() {
    if (!destroyed) {
      destroyed = true;
      scratch.recycle();
      if (ownsFile) {
        cacheFile.delete();
      }
    }
  }
}
