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
package org.flixelgdx.gradle.html5;

/**
 * Parses HTTP {@code Range} request headers for the HTML5 dev server.
 *
 * <p>Browsers need byte-range support to seek inside {@code <video>} and {@code <audio>}
 * elements. Only a single {@code bytes=} range is supported; anything else falls back to a full
 * response. This class is build-time code and is not part of the game runtime.
 */
final class RangeParser {

  /** Result meaning the header is absent, malformed, or multi-range, so the full file is served. */
  static final long[] FULL = null;

  private RangeParser() {}

  /**
   * Parses a {@code Range} header value against a file of the given size.
   *
   * <p>Supports {@code bytes=start-end}, {@code bytes=start-}, and the suffix form
   * {@code bytes=-N}. The returned end is inclusive and clamped to the last byte.
   *
   * @param header The raw header value, or {@code null} if the header is absent.
   * @param total The total file size in bytes.
   * @return {@code {start, end}} for a satisfiable range, {@link #FULL} if the full file should be
   *     served, or an empty array if the range is unsatisfiable (respond with 416).
   */
  static long[] parse(String header, long total) {
    if (header == null) {
      return FULL;
    }
    String h = header.trim();
    if (!h.regionMatches(true, 0, "bytes=", 0, 6)) {
      return FULL;
    }
    String spec = h.substring(6).trim();
    if (spec.indexOf(',') >= 0) {
      return FULL;
    }
    int dash = spec.indexOf('-');
    if (dash < 0) {
      return FULL;
    }
    String a = spec.substring(0, dash).trim();
    String b = spec.substring(dash + 1).trim();
    try {
      long start;
      long end;
      if (a.isEmpty()) {
        if (b.isEmpty()) {
          return FULL;
        }
        long suffix = Long.parseLong(b);
        if (suffix <= 0 || total == 0) {
          return new long[0];
        }
        start = Math.max(0, total - suffix);
        end = total - 1;
      } else {
        start = Long.parseLong(a);
        end = b.isEmpty() ? total - 1 : Long.parseLong(b);
        if (start < 0 || end < start) {
          return b.isEmpty() || start < 0 ? new long[0] : FULL;
        }
        if (start >= total) {
          return new long[0];
        }
        end = Math.min(end, total - 1);
      }
      return new long[] { start, end };
    } catch (NumberFormatException e) {
      return FULL;
    }
  }
}
