/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.jvm.logging;

import me.stringdotjar.flixelgdx.logging.FlixelStackFrame;
import me.stringdotjar.flixelgdx.logging.FlixelStackTraceProvider;

/**
 * Implementation of {@link FlixelStackTraceProvider} using Java's {@link StackWalker}.
 * This implementation is used for pretty much every platform excluding TeaVM, which doesn't support it.
 */
public class FlixelDefaultStackTraceProvider implements FlixelStackTraceProvider {

  /**
   * @return {@code true} if this frame may represent the real logging call site.
   */
  private static boolean isUsableCallerFrame(StackWalker.StackFrame f) {
    String className = f.getClassName();
    if ("me.stringdotjar.flixelgdx.logging.FlixelLogger".equals(className)) {
      return false;
    }
    if ("me.stringdotjar.flixelgdx.backend.jvm.logging.FlixelDefaultStackTraceProvider".equals(className)) {
      return false;
    }
    if ("me.stringdotjar.flixelgdx.Flixel".equals(className)) {
      // Skip static log facades so FlixelLogger sees the real caller (matches flixelgdx-logging-plugin skipping weaves there).
      String method = f.getMethodName();
      if ("info".equals(method) || "warn".equals(method) || "error".equals(method)) {
        return false;
      }
    }
    String pkg = f.getDeclaringClass().getPackageName();
    if (pkg.startsWith("org.codehaus.groovy.")) return false;
    if (pkg.startsWith("groovy.lang.")) return false;
    if (className.contains("$_run_closure")) return false;
    if (className.contains("$$Lambda$")) return false;
    if (pkg.startsWith("sun.reflect.") || pkg.startsWith("java.lang.reflect.")) return false;
    return true;
  }

  @Override
  public FlixelStackFrame getCaller() {
    return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
      .walk(frames -> frames.filter(FlixelDefaultStackTraceProvider::isUsableCallerFrame).findFirst())
      .map(StackWalkerFrame::new)
      .orElse(null);
  }

  private record StackWalkerFrame(StackWalker.StackFrame frame) implements FlixelStackFrame {

    @Override
    public String getFileName() {
      return frame.getFileName();
    }

    @Override
    public int getLineNumber() {
      return frame.getLineNumber();
    }

    @Override
    public String getClassName() {
      return frame.getClassName();
    }

    @Override
    public String getMethodName() {
      return frame.getMethodName();
    }
  }
}
