/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.asset;

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
