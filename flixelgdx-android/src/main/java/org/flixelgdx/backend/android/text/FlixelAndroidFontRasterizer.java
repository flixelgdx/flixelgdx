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

/**
 * The Android font rasterizer, built on the platform's native text stack (Typeface + Paint).
 *
 * <p>{@code Typeface.createFromFile} is the one loading path that works on every supported API
 * level, so the font bytes are written to a unique temporary file in the application's cache
 * directory, loaded, and the file is deleted immediately. The loaded typeface keeps its own copy
 * of the data, so nothing is left on disk.
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
    // Typeface can only be built from a file on older API levels, so the bytes go to a unique
    // temporary file. The typeface maps the font into memory while loading, so the file is
    // deleted right away and nothing is left behind in the cache directory.
    File fontFile = null;
    try {
      fontFile = File.createTempFile("flixelgdx_font_", ".ttf", context.getCacheDir());
      try (FileOutputStream out = new FileOutputStream(fontFile)) {
        out.write(data);
      }
      Typeface typeface = Typeface.createFromFile(fontFile);
      if (typeface == null) {
        return null;
      }
      return new FlixelAndroidRasterizedFont(typeface, pixelHeight);
    } catch (Exception e) {
      return null;
    } finally {
      if (fontFile != null) {
        fontFile.delete();
      }
    }
  }
}
