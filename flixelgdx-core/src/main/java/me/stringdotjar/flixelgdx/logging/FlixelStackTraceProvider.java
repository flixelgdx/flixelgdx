package me.stringdotjar.flixelgdx.logging;

/**
 * Interface for providing stack trace information in a platform-independent way.
 */
public interface FlixelStackTraceProvider {

  /**
   * Gets the stack frame of the caller of the logger.
   *
   * @return The stack frame of the caller, or {@code null} if it cannot be determined.
   */
  FlixelStackFrame getCaller();
}
