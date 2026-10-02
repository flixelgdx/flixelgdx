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
package org.flixelgdx.backend.desktop.logging;

import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogEntry;
import org.flixelgdx.logging.FlixelLogSite;
import org.flixelgdx.logging.FlixelLogSiteResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Finds the line of game code that made a log call by walking the JVM stack.
 *
 * <p>Think of the call stack as a stack of plates, where every plate is a method that is waiting for
 * the one above it to finish. When you log a message, the top plates all belong to the logger itself
 * (and to helpers such as {@code Flixel.info(...)}). This resolver peels those plates off until it
 * reaches the first plate that belongs to your code, and reports its file, line, class, and method.
 *
 * <p>The walk first looks for the frame of {@link FlixelDefaultLogger}. This is needed because the call
 * site is looked up lazily while a sink is writing (see {@link FlixelLogEntry#site()}), so frames from
 * sinks sit above the logger and must be ignored. After that frame, these frames are skipped:
 * <ul>
 *   <li>Every class that lives directly in the {@code org.flixelgdx.logging} package.</li>
 *   <li>The {@code debug}, {@code info}, {@code warn}, and {@code error} methods of
 *       {@code org.flixelgdx.Flixel}.</li>
 *   <li>Lambda proxy, reflection, and Groovy frames.</li>
 * </ul>
 *
 * <p>The first frame left over is the caller. If no {@link FlixelDefaultLogger} frame is on the stack
 * (for example, when a custom logger is installed), the site is reported as unknown.
 *
 * <p>This resolver is safe to use from several threads at once, because the walk keeps all of its state
 * in local variables. A stack walk creates some short-lived objects inside the JVM, which is the price
 * of finding the location without rewriting any bytecode.
 *
 * <p>Install it from the desktop launcher:
 * <pre>{@code
 * Flixel.log.setSiteResolver(new FlixelJvmLogSiteResolver());
 * }</pre>
 */
public class FlixelJvmLogSiteResolver implements FlixelLogSiteResolver {

  private static final String LOGGER_CLASS = FlixelDefaultLogger.class.getName();
  private static final String LOGGING_PACKAGE_PREFIX = "org.flixelgdx.logging.";
  private static final String FLIXEL_CLASS = "org.flixelgdx.Flixel";

  private static final StackWalker WALKER = StackWalker.getInstance();

  private static final Function<Stream<StackWalker.StackFrame>, StackWalker.StackFrame> FINDER = frames -> {
    Iterator<StackWalker.StackFrame> it = frames.iterator();
    boolean pastLogger = false;
    while (it.hasNext()) {
      StackWalker.StackFrame frame = it.next();
      String cls = frame.getClassName();
      if (!pastLogger) {
        pastLogger = cls.equals(LOGGER_CLASS);
        continue;
      }
      if (!isInternal(cls, frame.getMethodName())) {
        return frame;
      }
    }
    return null;
  };

  @Override
  public boolean resolve(@NotNull FlixelLogSite out) {
    StackWalker.StackFrame frame = WALKER.walk(FINDER);
    if (frame == null) {
      return false;
    }
    out.set(frame.getFileName(), frame.getLineNumber(), frame.getClassName(), frame.getMethodName());
    return true;
  }

  private static boolean isInternal(String cls, String method) {
    if (cls.startsWith(LOGGING_PACKAGE_PREFIX)) {
      // Only the logging package itself is skipped, not sub-packages such as a user's own sinks.
      return cls.indexOf('.', LOGGING_PACKAGE_PREFIX.length()) < 0;
    }
    if (cls.equals(FLIXEL_CLASS)) {
      return method.equals("debug") || method.equals("info") || method.equals("warn") || method.equals("error");
    }
    return cls.contains("$$Lambda")
        || cls.startsWith("java.lang.reflect.")
        || cls.startsWith("jdk.internal.reflect.")
        || cls.startsWith("sun.reflect.")
        || cls.startsWith("groovy.")
        || cls.startsWith("org.codehaus.groovy.")
        || cls.startsWith("org.apache.groovy.");
  }
}
