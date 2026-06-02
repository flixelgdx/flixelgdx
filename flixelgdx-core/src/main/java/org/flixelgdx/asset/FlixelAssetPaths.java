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
package org.flixelgdx.asset;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

/**
 * Canonical path helpers for internal asset keys used with {@link FlixelAssetManager} and libGDX file APIs.
 *
 * <p>Accidentally duplicated slashes (for example {@code "ui//mainmenu/bg.png"}) or backslashes can confuse loaders,
 * especially on HTML backends where manifest paths often match internal keys literally.
 *
 * <p>This helper collapses duplicate separators into one forward slash and maps {@code '\'} to {@code '/'}, matching the
 * common layout used inside {@code assets/}. Call sites include {@link FlixelDefaultAssetManager}.
 *
 * <p><b>Note:</b> This is aimed at internal resource paths such as {@code "fonts/foo.ttf"}, not arbitrary URLs or UNC paths.
 */
public final class FlixelAssetPaths {

  private FlixelAssetPaths() {}

  /**
   * Returns the same path with redundant slashes collapsed and backslashes converted to forward slashes.
   *
   * <p>If the input already looks canonical, the original reference may be returned to avoid allocating.
   *
   * @param path Internal asset path. Must not be {@code null}.
   * @return Canonical internal-style path (never {@code null}; empty stays empty).
   */
  @NotNull
  public static String normalizeAssetPath(@NotNull String path) {
    Objects.requireNonNull(path, "path cannot be null.");
    if (path.isEmpty()) {
      return path;
    }
    if (!needsNormalization(path)) {
      return path;
    }

    int n = path.length();
    StringBuilder sb = new StringBuilder(n);
    boolean prevSlash = false;
    for (int i = 0; i < n; i++) {
      char c = path.charAt(i);
      if (c == '\\') {
        c = '/';
      }
      if (c == '/') {
        if (prevSlash) {
          continue;
        }
        prevSlash = true;
      } else {
        prevSlash = false;
      }
      sb.append(c);
    }
    return sb.toString();
  }

  private static boolean needsNormalization(@NotNull String path) {
    int n = path.length();
    boolean prevSlash = false;
    for (int i = 0; i < n; i++) {
      char c = path.charAt(i);
      if (c == '\\') {
        return true;
      }
      if (c == '/') {
        if (prevSlash) {
          return true;
        }
        prevSlash = true;
      } else {
        prevSlash = false;
      }
    }
    return false;
  }
}
