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

/**
 * A {@link FlixelLogSiteResolver} that reads a call site which was recorded just before the log call.
 *
 * <p>Some platforms (the web, for example) cannot walk the stack to find out who called the logger.
 * Instead, a compiler plugin inserts a call to {@link #mark(String, int, String, String)} right before
 * every log call, leaving a sticky note with the file, line, class, and method. When a sink later asks
 * for {@link FlixelLogEntry#site()}, this resolver reads that note and throws it away so it cannot be
 * mistaken for the next message.
 *
 * <p>Game code never calls {@link #mark(String, int, String, String)} itself. It exists purely as the
 * hook that compiler plugins target. The logger also calls {@link #clear()} after every message (and
 * when a message is filtered out by level) so a stale note can never leak into a later one.
 *
 * <p>This resolver is meant for platforms that run game code on a single thread. The pending site is
 * shared by the whole process.
 */
public enum FlixelLogSiteMarker implements FlixelLogSiteResolver {

  /** The one shared resolver instance. */
  INSTANCE;

  private static int pendingLine;
  private static String pendingFile;
  private static String pendingClass;
  private static String pendingMethod;
  private static boolean pending;

  /**
   * Records the call site of the log call that is about to happen.
   *
   * <p>Inserted by a compiler plugin. Do not call this from game code.
   *
   * @param fileName The source file name, such as {@code MyState.java}.
   * @param line The source line number, or {@code 0} if unknown.
   * @param className The fully qualified name of the class that contains the log call.
   * @param methodName The simple name of the method that contains the log call.
   */
  public static void mark(String fileName, int line, String className, String methodName) {
    pendingFile = fileName;
    pendingLine = line;
    pendingClass = className;
    pendingMethod = methodName;
    pending = true;
  }

  /** Forgets any pending call site. */
  public static void clear() {
    pending = false;
    pendingFile = null;
    pendingClass = null;
    pendingMethod = null;
    pendingLine = 0;
  }

  /**
   * Copies the pending call site into {@code out} and clears it.
   *
   * @param out The holder to fill in.
   * @return {@code true} if a site was pending, {@code false} if nothing was marked.
   */
  @Override
  public boolean resolve(@NotNull FlixelLogSite out) {
    if (!pending) {
      return false;
    }
    out.set(pendingFile, pendingLine, pendingClass, pendingMethod);
    clear();
    return true;
  }
}
