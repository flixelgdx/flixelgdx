package me.stringdotjar.flixelgdx.backend.teavm.logging;

import me.stringdotjar.flixelgdx.logging.FlixelStackFrame;
import me.stringdotjar.flixelgdx.logging.FlixelStackTraceProvider;

/**
 * Implementation of {@link FlixelStackTraceProvider} for TeaVM.
 * Since TeaVM does not support stack traces, this implementation returns null.
 */
public class TeaVMStackTraceProvider implements FlixelStackTraceProvider {

  @Override
  public FlixelStackFrame getCaller() {
    return null;
  }
}
