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
package org.flixelgdx.file;

import org.flixelgdx.Flixel;
import org.flixelgdx.backend.FlixelHostIntegration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Shared helpers for {@link FlixelHostIntegration} file picker implementations.
 *
 * <p>Game code normally never needs this class. Backends use it to clean up the extension filter
 * the game passed in and to hand results back on the main thread.
 */
public final class FlixelFilePicker {

  /** Shared empty filter meaning "any file may be chosen". */
  public static final String[] NO_FILTER = new String[0];

  /** Shared empty result delivered on cancel or failure. */
  public static final FlixelFile[] NO_FILES = new FlixelFile[0];

  private FlixelFilePicker() {}

  /**
   * Cleans an extension filter so every backend sees the same format.
   *
   * <p>Entries are trimmed and lower-cased, and a leading {@code *.} or {@code .} is removed, so
   * {@code ".PNG"}, {@code "*.png"}, and {@code "png"} all become {@code "png"}. Blank entries are
   * dropped. A bare {@code "*"} means any file, which turns the whole filter into {@link #NO_FILTER}.
   *
   * @param extensions The extensions the game asked for; may be {@code null} or empty.
   * @return The normalized extensions, or {@link #NO_FILTER} if any file is allowed; never {@code null}.
   */
  @NotNull
  public static String[] normalize(@Nullable String[] extensions) {
    if (extensions == null || extensions.length == 0) {
      return NO_FILTER;
    }
    int count = 0;
    String[] out = new String[extensions.length];
    for (int i = 0; i < extensions.length; i++) {
      String ext = extensions[i];
      if (ext == null) {
        continue;
      }
      ext = ext.trim().toLowerCase(Locale.ROOT);
      if (ext.startsWith("*.")) {
        ext = ext.substring(2);
      } else if (ext.startsWith(".")) {
        ext = ext.substring(1);
      }
      if (ext.equals("*")) {
        return NO_FILTER;
      }
      if (!ext.isEmpty()) {
        out[count++] = ext;
      }
    }
    if (count == 0) {
      return NO_FILTER;
    }
    if (count == out.length) {
      return out;
    }
    String[] trimmed = new String[count];
    System.arraycopy(out, 0, trimmed, 0, count);
    return trimmed;
  }

  /**
   * Checks whether a file name passes an already normalized extension filter.
   *
   * @param name The file name to test.
   * @param extensions Extensions from {@link #normalize(String[])}; empty accepts everything.
   * @return {@code true} if the name ends in one of the extensions or the filter is empty.
   */
  public static boolean matches(@NotNull String name, @NotNull String[] extensions) {
    if (extensions.length == 0) {
      return true;
    }
    int dot = name.lastIndexOf('.');
    if (dot < 0 || dot == name.length() - 1) {
      return false;
    }
    int len = name.length() - dot - 1;
    for (int i = 0; i < extensions.length; i++) {
      if (extensions[i].length() == len && name.regionMatches(true, dot + 1, extensions[i], 0, len)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Delivers picked files to a listener on the game's main thread.
   *
   * @param listener The listener to notify.
   * @param files The result to hand over; {@code null} is treated as a cancel.
   */
  public static void deliver(@NotNull FlixelFilePickListener listener, @Nullable FlixelFile[] files) {
    final FlixelFile[] result = files != null ? files : NO_FILES;
    Flixel.graphics.queueMainThread(() -> listener.onPick(result));
  }
}
