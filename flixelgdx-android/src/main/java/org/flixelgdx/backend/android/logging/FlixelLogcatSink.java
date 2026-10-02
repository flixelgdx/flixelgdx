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
import org.flixelgdx.logging.FlixelLogEntry;
import org.flixelgdx.logging.FlixelLogFormat;
import org.flixelgdx.logging.FlixelLogLevel;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogSink;
import org.flixelgdx.logging.FlixelLogSite;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link FlixelLogSink} that sends every message to Android's Logcat.
 *
 * <p>Logcat is Android's built-in message board: every app pins notes to it, and tools such as
 * Android Studio read them back. This sink pins one note per log message, using the matching Logcat
 * priority so that Android Studio's level filters and colors work: {@link FlixelLogLevel#DEBUG} becomes
 * {@link Log#DEBUG}, {@link FlixelLogLevel#INFO} becomes {@link Log#INFO}, {@link FlixelLogLevel#WARN}
 * becomes {@link Log#WARN}, and {@link FlixelLogLevel#ERROR} becomes {@link Log#ERROR}.
 *
 * <p>The Logcat tag is the tag of the logger that was called, or the default tag
 * ({@code FlixelGDX} unless changed with {@link #setDefaultTag(String)}) when that tag is empty. The
 * text starts with the location of the log call, followed by the message and, when an exception is
 * attached, its stack trace. Logcat records the time itself, so no timestamp is added. The location
 * follows the logger's mode:
 * <ul>
 *   <li>{@link FlixelLogMode#SIMPLE}: {@code pkg/path/File.java:42: message}</li>
 *   <li>{@link FlixelLogMode#DETAILED}: {@code [File.java:42] [method()] message}</li>
 * </ul>
 *
 * <p>Logcat cuts a single note off at roughly 4000 characters, which a long stack trace can easily
 * exceed. Text longer than that is split into several notes at line breaks where possible, and each
 * piece is sent with the same priority and tag. Android's {@link Log} class only accepts
 * {@link String}s, so each Logcat call creates one string; everything before that call reuses a
 * single buffer.
 *
 * <p>Install it from the launcher before the game starts:
 *
 * <pre>{@code
 * Flixel.log.setConsoleSink(new FlixelLogcatSink());
 * }</pre>
 */
public class FlixelLogcatSink implements FlixelLogSink {

  /** The largest piece, in characters, that is sent to Logcat in a single call. */
  public static final int MAX_CHUNK = 3500;

  private static final String FALLBACK_TAG = "FlixelGDX";

  private final FlixelString line = new FlixelString(512);

  private String defaultTag;

  /** Creates a sink that uses {@code FlixelGDX} as the Logcat tag when a logger has no tag. */
  public FlixelLogcatSink() {
    this(FALLBACK_TAG);
  }

  /**
   * Creates a sink with a custom default Logcat tag.
   *
   * @param defaultTag The Logcat tag to use when a logger has no tag. An empty or {@code null} value
   *   means {@code FlixelGDX}.
   */
  public FlixelLogcatSink(String defaultTag) {
    this.defaultTag = normalize(defaultTag);
  }

  @Override
  public void write(@NotNull FlixelLogEntry entry) {
    int priority = priorityOf(entry.getLevel());
    String tag = entry.getTag();
    if (tag.isEmpty()) {
      tag = defaultTag;
    }

    line.clear();
    if (entry.getMode() == FlixelLogMode.DETAILED) {
      FlixelLogSite site = entry.site();
      line.concat('[');
      line.concat(site.getFileName());
      line.concat(':');
      line.concat(site.getLine());
      line.concat(']');
      line.concat(' ');
      line.concat('[');
      line.concat(site.getMethodName());
      line.concat('(');
      line.concat(')');
      line.concat(']');
      line.concat(' ');
      line.concat(entry.getMessage());
    } else {
      FlixelLogFormat.appendSimple(line, entry);
    }
    Throwable t = entry.getThrowable();
    if (t != null) {
      FlixelLogFormat.appendThrowable(line, t);
    }

    int len = line.length();
    if (len <= MAX_CHUNK) {
      Log.println(priority, tag, line.toString());
      return;
    }
    char[] chars = line.charBuffer().getItems();
    int start = 0;
    while (start < len) {
      int end = Math.min(start + MAX_CHUNK, len);
      int next = end;
      if (end < len) {
        int nl = end;
        while (nl > start && chars[nl] != '\n') {
          nl--;
        }
        if (nl > start) {
          end = nl;
          next = nl + 1;
        } else if (Character.isHighSurrogate(chars[end - 1])) {
          // Never cut a surrogate pair in half.
          end--;
          next = end;
        }
      }
      if (end > start) {
        Log.println(priority, tag, new String(chars, start, end - start));
      }
      start = next;
    }
  }

  private static String normalize(String tag) {
    return tag == null || tag.isEmpty() ? FALLBACK_TAG : tag;
  }

  private static int priorityOf(@NotNull FlixelLogLevel level) {
    return switch (level) {
      case DEBUG -> Log.DEBUG;
      case INFO -> Log.INFO;
      case WARN -> Log.WARN;
      case ERROR -> Log.ERROR;
    };
  }

  /**
   * Returns the Logcat tag used when a logger has no tag of its own.
   *
   * @return The default tag, never empty.
   */
  @NotNull
  public String getDefaultTag() {
    return defaultTag;
  }

  /**
   * Sets the Logcat tag used when a logger has no tag of its own.
   *
   * @param defaultTag The new default tag. An empty or {@code null} value means {@code FlixelGDX}.
   */
  public void setDefaultTag(String defaultTag) {
    this.defaultTag = normalize(defaultTag);
  }
}
