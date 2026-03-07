package me.stringdotjar.flixelgdx.backend.jvm.logging;

import me.stringdotjar.flixelgdx.logging.FlixelStackFrame;
import me.stringdotjar.flixelgdx.logging.FlixelStackTraceProvider;
import me.stringdotjar.flixelgdx.util.FlixelRuntimeUtil;

/**
 * Implementation of {@link FlixelStackTraceProvider} using Java's {@link StackWalker}.
 * This implementation is used for pretty much every platform excluding TeaVM, which doesn't support it.
 */
public class FlixelDefaultStackTraceProvider implements FlixelStackTraceProvider {

  @Override
  public FlixelStackFrame getCaller() {
    return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
      .walk(frames -> frames.filter(f -> {
        // Filter packages to not log.
        String pkg = f.getDeclaringClass().getPackageName();
        if (pkg.startsWith(FlixelRuntimeUtil.getLibraryRoot())) return false; // Do not include FlixelGDX logs.
        if (pkg.startsWith("org.codehaus.groovy.")) return false;
        if (pkg.startsWith("groovy.lang.")) return false;
        if (pkg.contains("$_run_closure")) return false;
        if (pkg.contains("$$Lambda$")) return false;
        if (pkg.startsWith("sun.reflect.") || pkg.startsWith("java.lang.reflect.")) return false;
        return true;
      }).findFirst())
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
