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
package org.flixelgdx.logging;

import org.flixelgdx.Flixel;
import org.flixelgdx.backend.FlixelNoopRuntimeDevice;
import org.flixelgdx.backend.FlixelRuntimeDevice;
import org.flixelgdx.util.FlixelExceptionUtil;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Shared text layouts for log lines, so every sink prints the same thing.
 *
 * <p>Sinks that print or store text build their line with these helpers instead of each inventing its
 * own layout. Every method appends to a {@link FlixelString} that the caller owns and reuses, so
 * formatting a line does not create garbage (apart from the explicitly noted exceptions).
 *
 * <p>Example:
 * <pre>{@code
 * private final FlixelString line = new FlixelString(256);
 *
 * public void write(FlixelLogEntry entry) {
 *   line.clear();
 *   FlixelLogFormat.appendDetailed(line, entry);
 *   System.out.println(line);
 * }
 * }</pre>
 */
public final class FlixelLogFormat {

  private static final long HOUR_MS = 3_600_000L;
  private static final long MINUTE_MS = 60_000L;

  private static long hourStart = Long.MAX_VALUE;

  private static int hourOfDay;

  private static FlixelRuntimeDevice cachedDevice;

  private static final Object TIME_LOCK = new Object();
  private static final char[] dateChars = new char[10];

  private FlixelLogFormat() {}

  /**
   * Appends the short layout: {@code pkg/path/File.java:42: message}.
   *
   * <p>The package path comes from the call site's class name, with each dot turned into a slash. When
   * the class has no package, the line starts with just {@code File.java:42: }.
   *
   * @param out The string to append to.
   * @param e The entry to format.
   */
  public static void appendSimple(@NotNull FlixelString out, @NotNull FlixelLogEntry e) {
    FlixelLogSite site = e.site();
    String cls = site.getClassName();
    int lastDot = cls.lastIndexOf('.');
    if (lastDot > 0) {
      for (int i = 0; i < lastDot; i++) {
        char c = cls.charAt(i);
        out.concat(c == '.' ? '/' : c);
      }
      out.concat('/');
    }
    out.concat(site.getFileName());
    out.concat(':');
    out.concat(site.getLine());
    out.concat(':');
    out.concat(' ');
    out.concat(e.getMessage());
  }

  /**
   * Appends the detailed layout:
   * {@code yyyy-MM-dd HH:mm:ss.SSS [LEVEL] [tag] [File.java:42] [method()] message}.
   *
   * @param out The string to append to.
   * @param e The entry to format.
   */
  public static void appendDetailed(@NotNull FlixelString out, @NotNull FlixelLogEntry e) {
    FlixelLogSite site = e.site();
    appendTimestamp(out, e.getTime());
    out.concat(' ');
    out.concat('[');
    out.concat(e.getLevel().name());
    out.concat(']');
    out.concat(' ');
    out.concat('[');
    out.concat(e.getTag());
    out.concat(']');
    out.concat(' ');
    out.concat('[');
    out.concat(site.getFileName());
    out.concat(':');
    out.concat(site.getLine());
    out.concat(']');
    out.concat(' ');
    out.concat('[');
    out.concat(site.getMethodName());
    out.concat('(');
    out.concat(')');
    out.concat(']');
    out.concat(' ');
    out.concat(e.getMessage());
  }

  /**
   * Appends a local-time timestamp in the form {@code yyyy-MM-dd HH:mm:ss.SSS}.
   *
   * <p>The date and the hour are cached and only recomputed when the time moves into a different hour
   * (which also keeps daylight saving changes correct). The local time zone comes from
   * {@link FlixelRuntimeDevice#getUtcOffsetMillis(long)}, so each platform decides what "local" means
   * (a browser build reports the browser's time zone, not UTC). The cache is also refreshed when
   * {@link Flixel#runtime} is replaced, so a timestamp logged before a backend installs its device
   * does not keep the old offset. Minutes, seconds, and milliseconds are worked
   * out with plain arithmetic, so the usual call creates no objects. The refresh once per hour does.
   *
   * @param out The string to append to.
   * @param epochMillis The time in milliseconds since the Unix epoch.
   */
  public static void appendTimestamp(@NotNull FlixelString out, long epochMillis) {
    int hour;
    long rem;
    synchronized (TIME_LOCK) {
      long delta = epochMillis - hourStart;
      if (delta < 0L || delta >= HOUR_MS || cachedDevice != currentDevice()) {
        refreshHour(epochMillis);
        delta = epochMillis - hourStart;
      }
      rem = delta;
      hour = hourOfDay;
      for (int i = 0; i < 10; i++) {
        out.concat(dateChars[i]);
      }
    }
    out.concat(' ');
    pad(out, hour, 2);
    out.concat(':');
    pad(out, (int) (rem / MINUTE_MS), 2);
    out.concat(':');
    pad(out, (int) ((rem % MINUTE_MS) / 1000L), 2);
    out.concat('.');
    pad(out, (int) (rem % 1000L), 3);
  }

  /**
   * Appends a line break followed by the full details of an exception, including its stack trace and
   * its causes.
   *
   * <p>This builds a temporary string, which is acceptable because it only happens when a message
   * actually carries an exception.
   *
   * @param out The string to append to.
   * @param t The exception to describe.
   * @see FlixelExceptionUtil#getFullExceptionMessage(Throwable)
   */
  public static void appendThrowable(@NotNull FlixelString out, @NotNull Throwable t) {
    out.concat('\n');
    out.concat(FlixelExceptionUtil.getFullExceptionMessage(t));
    int len = out.length();
    while (len > 0 && out.charAt(len - 1) == '\n') {
      len--;
    }
    out.setLength(len);
  }

  /** Forgets the cached hour so the next timestamp recomputes it. Tests call this after changing the offset source. */
  static void resetCache() {
    synchronized (TIME_LOCK) {
      hourStart = Long.MAX_VALUE;
      cachedDevice = null;
    }
  }

  private static FlixelRuntimeDevice currentDevice() {
    // Flixel.runtime is never null in normal use, but it is a public field, so fall back to the default.
    FlixelRuntimeDevice device = Flixel.runtime;
    return device != null ? device : FlixelNoopRuntimeDevice.INSTANCE;
  }

  private static void refreshHour(long epochMillis) {
    FlixelRuntimeDevice device = currentDevice();
    cachedDevice = device;
    long localMs = epochMillis + device.getUtcOffsetMillis(epochMillis);
    // Reading the shifted time as if it were UTC gives the local wall clock fields.
    LocalDateTime ldt = LocalDateTime.ofEpochSecond(
        Math.floorDiv(localMs, 1000L), Math.floorMod(localMs, 1000) * 1_000_000, ZoneOffset.UTC);
    hourStart = epochMillis - (ldt.getMinute() * MINUTE_MS + ldt.getSecond() * 1000L + ldt.getNano() / 1_000_000L);
    hourOfDay = ldt.getHour();
    int year = ldt.getYear();
    dateChars[0] = (char) ('0' + (year / 1000) % 10);
    dateChars[1] = (char) ('0' + (year / 100) % 10);
    dateChars[2] = (char) ('0' + (year / 10) % 10);
    dateChars[3] = (char) ('0' + year % 10);
    dateChars[4] = '-';
    dateChars[5] = (char) ('0' + ldt.getMonthValue() / 10);
    dateChars[6] = (char) ('0' + ldt.getMonthValue() % 10);
    dateChars[7] = '-';
    dateChars[8] = (char) ('0' + ldt.getDayOfMonth() / 10);
    dateChars[9] = (char) ('0' + ldt.getDayOfMonth() % 10);
  }

  private static void pad(FlixelString out, int value, int width) {
    int divisor = width == 3 ? 100 : 10;
    while (divisor > 0) {
      out.concat((char) ('0' + (value / divisor) % 10));
      divisor /= 10;
    }
  }
}
