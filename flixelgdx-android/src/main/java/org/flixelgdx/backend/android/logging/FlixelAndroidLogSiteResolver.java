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

import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogEntry;
import org.flixelgdx.logging.FlixelLogSite;
import org.flixelgdx.logging.FlixelLogSiteResolver;
import org.jetbrains.annotations.NotNull;

/**
 * Finds the call site of a log message on Android by reading the current thread's stack trace.
 *
 * <p>Think of it as retracing footsteps: the logger leaves a trail of its own frames behind it, and
 * this resolver walks that trail backward until it reaches the first frame that does not belong to the
 * logging system. That frame is the game code that asked for the message.
 *
 * <p>{@code StackWalker} needs API 26, so this class uses {@code new Throwable().getStackTrace()},
 * which works on every API level that FlixelGDX supports. Building a stack trace is not free, but it
 * only happens when a sink actually reads {@link FlixelLogEntry#site()}.
 *
 * <p>The call site is looked up lazily from inside a sink's {@code write(...)} method, so the stack
 * (innermost frame first) looks like this:
 * <ol>
 *   <li>this resolver and {@link FlixelLogEntry#site()},</li>
 *   <li>the sink's own {@code write(...)} method (it can live in any package, including a user's),</li>
 *   <li>the frames of {@link FlixelDefaultLogger} that dispatch the message,</li>
 *   <li>the logger's convenience methods and the static {@code Flixel.info(...)} style shortcuts,</li>
 *   <li>the game code that logged the message.</li>
 * </ol>
 *
 * <p>Install it from the launcher before the game starts:
 *
 * <pre>{@code
 * Flixel.log.setSiteResolver(new FlixelAndroidLogSiteResolver());
 * }</pre>
 */
public class FlixelAndroidLogSiteResolver implements FlixelLogSiteResolver {

  private static final String DEFAULT_LOGGER = "org.flixelgdx.logging.FlixelDefaultLogger";
  private static final String LOGGING_PACKAGE = "org.flixelgdx.logging.";
  private static final String FLIXEL = "org.flixelgdx.Flixel";

  @Override
  public boolean resolve(@NotNull FlixelLogSite out) {
    StackTraceElement[] frames = new Throwable().getStackTrace();
    int n = frames.length;
    int i = 0;
    while (i < n && !DEFAULT_LOGGER.equals(frames[i].getClassName())) {
      i++;
    }
    for (; i < n; i++) {
      StackTraceElement frame = frames[i];
      if (isInternal(frame)) {
        continue;
      }
      out.set(frame.getFileName(), frame.getLineNumber(), frame.getClassName(), frame.getMethodName());
      return true;
    }
    return false;
  }

  /**
   * Checks whether a frame belongs to the logging machinery (or to compiler and runtime plumbing)
   * instead of to the code that logged the message.
   *
   * @param frame The frame to inspect.
   * @return {@code true} if the frame must be skipped.
   */
  private static boolean isInternal(@NotNull StackTraceElement frame) {
    String cls = frame.getClassName();
    if (cls.startsWith(LOGGING_PACKAGE) && cls.indexOf('.', LOGGING_PACKAGE.length()) < 0) {
      return true;
    }
    if (FLIXEL.equals(cls)) {
      String m = frame.getMethodName();
      return "debug".equals(m) || "info".equals(m) || "warn".equals(m) || "error".equals(m);
    }
    return cls.contains("$$Lambda")
        || cls.contains("$$ExternalSynthetic")
        || cls.startsWith("java.lang.reflect.")
        || cls.startsWith("jdk.internal.reflect.")
        || cls.startsWith("sun.reflect.")
        || cls.startsWith("libcore.reflect.");
  }
}
