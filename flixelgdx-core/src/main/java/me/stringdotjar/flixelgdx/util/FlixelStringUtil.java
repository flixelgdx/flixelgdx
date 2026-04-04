/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.util;

/**
 * Handy utility class for handling strings more efficiently.
 */
public final class FlixelStringUtil {

  /**
   * Compares the content of two {@link CharSequence}s for equality. This works for
   * {@link String}s, {@link StringBuilder}s, and any {@link CharSequence}s.
   *
   * @param a The first {@link CharSequence} to compare.
   * @param b The second {@link CharSequence} to compare.
   * @return {@code true} if the content of the two {@link CharSequence}s is equal, {@code false} otherwise.
   */
  public static boolean contentEquals(CharSequence a, CharSequence b) {
    if (a == b) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }

    int len = a.length();
    if (len != b.length()) {
      return false;
    }
    for (int i = 0; i < len; i++) {
      if (a.charAt(i) != b.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  private FlixelStringUtil() {}
}
