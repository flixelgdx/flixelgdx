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

import android.content.Context;
import android.graphics.Typeface;
import org.flixelgdx.text.FlixelFontRasterizer;
import org.flixelgdx.text.FlixelRasterizedFont;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

/**
 * The Android font rasterizer, built on the platform's native text stack (Typeface + Paint).
 *
 * <p>Font bytes are written once to the application's cache directory under a stable name
 * derived from the content hash so the same font data is not written twice across calls within
 * one session. The file is removed when the returned {@link FlixelAndroidRasterizedFont} is
 * destroyed, but only when this opener created it.
 *
 * <p>On API 24 through 25 the file-based {@code Typeface.createFromFile} path is used.
 * On API 26 and above the same path is still used; an alternate builder-based path is
 * available from that API but offers no practical advantage here since the bytes are already
 * on disk for the duration of the font's use.
 *
 * @see FlixelAndroidRasterizedFont
 */
public class FlixelAndroidFontRasterizer implements FlixelFontRasterizer {

  private final Context context;

  /**
   * Creates a rasterizer that writes temporary font files into the application cache directory.
   *
   * @param context Any context; the application context is stored internally to avoid leaks.
   */
  public FlixelAndroidFontRasterizer(@NotNull Context context) {
    this.context = context.getApplicationContext();
  }

  @Nullable
  @Override
  public FlixelRasterizedFont open(byte @NotNull [] data, float pixelHeight) {
    File cacheDir = context.getCacheDir();
    // Build a stable filename from a 32-bit hash of the font bytes. Collisions are astronomically
    // unlikely in practice; the worst outcome is two different fonts sharing one file, which
    // Typeface.createFromFile would open incorrectly, and the rasterizer would produce wrong glyphs
    // rather than crash.
    String hex = Integer.toHexString(Arrays.hashCode(data) & 0x7FFFFFFF);
    File fontFile = new File(cacheDir, "flixelgdx_font_" + hex + ".ttf");
    boolean created = !fontFile.exists();
    if (created) {
      try (FileOutputStream out = new FileOutputStream(fontFile)) {
        out.write(data);
      } catch (Exception e) {
        return null;
      }
    }
    Typeface typeface;
    try {
      typeface = Typeface.createFromFile(fontFile);
    } catch (Exception e) {
      if (created) {
        fontFile.delete();
      }
      return null;
    }
    if (typeface == null) {
      if (created) {
        fontFile.delete();
      }
      return null;
    }
    return new FlixelAndroidRasterizedFont(typeface, fontFile, created, pixelHeight);
  }
}
