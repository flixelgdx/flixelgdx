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

import org.flixelgdx.logging.FlixelStackFrame;
import org.flixelgdx.logging.FlixelStackTraceProvider;
import org.jetbrains.annotations.Nullable;

/**
 * Android {@link FlixelStackTraceProvider} using {@link Throwable#getStackTrace()}.
 *
 * <p>Filters out logging-framework frames so the reported location is the real call site in game
 * code. StackWalker is not used here because it requires API 26; this implementation works on
 * API 24 and above.
 */
public class FlixelAndroidStackTraceProvider implements FlixelStackTraceProvider {

  @Nullable
  @Override
  public FlixelStackFrame getCaller() {
    StackTraceElement[] frames = new Throwable().getStackTrace();
    for (StackTraceElement frame : frames) {
      if (isUsableFrame(frame)) {
        return new AndroidStackFrame(frame);
      }
    }
    return null;
  }

  /**
   * Returns {@code true} when this frame represents the real logging call site, not an internal
   * logging layer.
   *
   * @param frame The frame to inspect.
   * @return {@code true} if the frame should be reported as the caller.
   */
  private static boolean isUsableFrame(StackTraceElement frame) {
    String className = frame.getClassName();
    if ("org.flixelgdx.logging.FlixelLogger".equals(className)) {
      return false;
    }
    if (className.startsWith("org.flixelgdx.backend.android.logging.")) {
      return false;
    }
    if ("org.flixelgdx.Flixel".equals(className)) {
      String method = frame.getMethodName();
      if ("info".equals(method) || "warn".equals(method)
          || "error".equals(method) || "debug".equals(method)) {
        return false;
      }
    }
    String pkg = className.contains(".")
        ? className.substring(0, className.lastIndexOf('.'))
        : "";
    if (pkg.startsWith("sun.reflect.") || pkg.startsWith("java.lang.reflect.")) {
      return false;
    }
    if (className.contains("$$Lambda$") || className.contains("$_run_closure")) {
      return false;
    }
    return true;
  }

  /** Wraps a {@link StackTraceElement} as a {@link FlixelStackFrame}. */
  private static final class AndroidStackFrame implements FlixelStackFrame {

    @Nullable
    private final StackTraceElement frame;

    private AndroidStackFrame(@Nullable StackTraceElement frame) {
      this.frame = frame;
    }

    @Override
    public String getFileName() {
      return frame != null ? frame.getFileName() : null;
    }

    @Override
    public int getLineNumber() {
      return frame != null ? frame.getLineNumber() : -1;
    }

    @Override
    public String getClassName() {
      return frame != null ? frame.getClassName() : null;
    }

    @Override
    public String getMethodName() {
      return frame != null ? frame.getMethodName() : null;
    }
  }
}
