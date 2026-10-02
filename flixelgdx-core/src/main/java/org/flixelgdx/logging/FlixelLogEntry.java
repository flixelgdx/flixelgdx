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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One log message, handed to every {@link FlixelLogSink} by the logger.
 *
 * <p>Think of an entry as a whiteboard in a meeting room: the logger writes a message on it, lets
 * every sink read it, then wipes it clean for the next message. Because the same entry (and the same
 * message text buffer) is reused for every log call so that logging does not create garbage, an entry
 * is <strong>only valid during the sink call</strong>. If you need to keep anything, copy it before
 * returning. For example, call {@code getMessage().toString()} to get a stable string.
 *
 * <p>The call site (file, line, method) is looked up lazily by {@link #site()}, so sinks that never
 * show it pay nothing for it.
 */
public final class FlixelLogEntry {

  long time;

  FlixelLogLevel level = FlixelLogLevel.INFO;
  FlixelLogMode mode = FlixelLogMode.SIMPLE;
  String tag = "";
  CharSequence message;
  Throwable throwable;
  FlixelLogSiteResolver resolver;

  private final FlixelLogSite site = new FlixelLogSite();

  boolean siteResolved;

  FlixelLogEntry(@NotNull CharSequence message) {
    this.message = message;
  }

  /**
   * Prepares this entry for a new message. The message text is not touched.
   *
   * @param time The time of the message, in milliseconds since the Unix epoch.
   * @param level The level of the message.
   * @param mode The console format that the logger is using for this message.
   * @param tag The tag of the logger that was called.
   * @param throwable The attached exception, or {@code null}.
   * @param resolver The resolver used by {@link #site()}, or {@code null} for none.
   */
  void reset(
      long time,
      @NotNull FlixelLogLevel level,
      @NotNull FlixelLogMode mode,
      @NotNull String tag,
      @Nullable Throwable throwable,
      @Nullable FlixelLogSiteResolver resolver) {
    this.time = time;
    this.level = level;
    this.mode = mode;
    this.tag = tag;
    this.throwable = throwable;
    this.resolver = resolver;
    this.siteResolved = false;
  }

  /**
   * Returns where in the source code the log call was made.
   *
   * <p>The lookup runs the first time this is called for an entry (using the logger's
   * {@link FlixelLogSiteResolver}) and the answer is remembered for the rest of the sink calls for the
   * same message. If the site cannot be found, the file is {@code "Unknown"}, the line is {@code 0},
   * the class name is empty, and the method is {@code "unknown"}.
   *
   * @return The reused call site holder, never {@code null}. Valid only during the sink call.
   */
  @NotNull
  public FlixelLogSite site() {
    if (!siteResolved) {
      siteResolved = true;
      boolean found = false;
      FlixelLogSiteResolver r = resolver;
      if (r != null) {
        try {
          found = r.resolve(site);
        } catch (RuntimeException e) {
          found = false;
        }
      }
      if (!found) {
        site.clear();
      }
    }
    return site;
  }

  /**
   * Returns when the message was logged.
   *
   * @return The time in milliseconds since the Unix epoch.
   */
  public long getTime() {
    return time;
  }

  /**
   * Returns the level of the message.
   *
   * @return The level, never {@code null}.
   */
  @NotNull
  public FlixelLogLevel getLevel() {
    return level;
  }

  /**
   * Returns the console format that the logger was using when the message was logged.
   *
   * @return The mode, never {@code null}.
   */
  @NotNull
  public FlixelLogMode getMode() {
    return mode;
  }

  /**
   * Returns the tag of the logger that was called.
   *
   * @return The tag, never {@code null} but possibly empty.
   */
  @NotNull
  public String getTag() {
    return tag;
  }

  /**
   * Returns the fully formatted message text, with every {@code {}} already filled in.
   *
   * <p>This is a reused buffer. Valid only during the sink call. Call {@code toString()} on it to keep a copy.
   *
   * @return The message text, never {@code null}.
   */
  @NotNull
  public CharSequence getMessage() {
    return message;
  }

  /**
   * Returns the exception attached to the message, if any.
   *
   * @return The exception, or {@code null} when the message has none.
   */
  @Nullable
  public Throwable getThrowable() {
    return throwable;
  }

  /**
   * Builds a readable one-line description. This creates new objects, so do not use it in hot paths.
   *
   * @return The level, the tag (when present), and the message.
   */
  @Override
  @NotNull
  public String toString() {
    return "[" + level + "] " + (tag.isEmpty() ? "" : "[" + tag + "] ") + message;
  }
}
