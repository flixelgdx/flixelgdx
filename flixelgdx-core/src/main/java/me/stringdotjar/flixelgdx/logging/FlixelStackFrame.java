package me.stringdotjar.flixelgdx.logging;

/**
 * Interface representing a stack frame in a platform-independent way.
 */
public interface FlixelStackFrame {

  /** @return The name of the file containing the execution point represented by this stack frame. */
  String getFileName();

  /** @return The line number of the execution point represented by this stack frame. */
  int getLineNumber();

  /** @return The fully qualified name of the class containing the execution point represented by this stack frame. */
  String getClassName();

  /** @return The name of the method containing the execution point represented by this stack frame. */
  String getMethodName();
}
