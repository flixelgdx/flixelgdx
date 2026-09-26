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
package org.flixelgdx.backend.android.logging;

import android.util.Log;
import org.flixelgdx.logging.FlixelLogConsoleSink;
import org.flixelgdx.logging.FlixelLogLevel;

/**
 * Sends the framework's console log lines to Logcat instead of {@code System.out}.
 *
 * <p>Printing to {@code System.out} on Android does reach Logcat, but every line arrives as an
 * {@code I/System.out} entry with the desktop's ANSI color codes left in as raw text, and warnings
 * and errors cannot be filtered by priority. This sink writes each line through
 * {@link Log} with the matching priority ({@code INFO}, {@code WARN}, or {@code ERROR}) and the
 * log tag as the Logcat tag, with no color codes, so the output reads cleanly in Android Studio
 * and {@code adb logcat}.
 *
 * <p>The Android launcher installs this automatically. File logging is unaffected; it is handled
 * separately by {@link FlixelAndroidLogFileHandler}.
 */
public final class FlixelAndroidLogConsoleSink implements FlixelLogConsoleSink {

  /** Logcat tag used when a log call has no tag of its own. */
  private static final String DEFAULT_TAG = "FlixelGDX";

  /** Reused for each line so logging does not build a new builder per message. */
  private final StringBuilder line = new StringBuilder(256);

  @Override
  public synchronized void emit(
      FlixelLogLevel level,
      String tag,
      String message,
      String simpleLocation,
      String detailedFile,
      String methodLabel,
      String timestamp,
      boolean detailed) {
    // Logcat already records the time, so only the location and message are included.
    line.setLength(0);
    if (detailed) {
      line.append('[').append(detailedFile).append("] [").append(methodLabel).append("] ");
    } else {
      line.append(simpleLocation).append(' ');
    }
    line.append(message);
    String logTag = tag == null || tag.isEmpty() ? DEFAULT_TAG : tag;
    Log.println(priorityOf(level), logTag, line.toString());
  }

  /**
   * Maps a framework log level to the matching Logcat priority.
   *
   * @param level The framework log level.
   * @return The {@link Log} priority constant.
   */
  private static int priorityOf(FlixelLogLevel level) {
    return switch (level) {
      case WARN -> Log.WARN;
      case ERROR -> Log.ERROR;
      default -> Log.INFO;
    };
  }
}
