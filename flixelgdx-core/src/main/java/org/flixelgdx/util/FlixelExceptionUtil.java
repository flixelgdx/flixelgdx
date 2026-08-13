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
package org.flixelgdx.util;

import org.jetbrains.annotations.NotNull;

/**
 * Small helpers for handling exceptions and stack traces.
 */
public final class FlixelExceptionUtil {

  /**
   * Obtains a string representation of where an exception was thrown from, including the class,
   * method, file, and line number.
   *
   * @param exception The exception to obtain the location from.
   * @return A string representation of where the exception was thrown from.
   */
  public static @NotNull String getExceptionLocation(Throwable exception) {
    if (exception == null) {
      return "Unknown Location";
    }
    StackTraceElement[] stackTrace = exception.getStackTrace();
    if (stackTrace.length == 0) {
      return "Unknown Location";
    }
    StackTraceElement element = stackTrace[0];
    return "FILE="
        + element.getFileName()
        + ", CLASS="
        + element.getClassName()
        + ", METHOD="
        + element.getMethodName()
        + "(), LINE="
        + element.getLineNumber();
  }

  /**
   * Obtains a full detailed message from an exception, including its type, location, and stack trace.
   *
   * @param exception The exception to obtain the message from.
   * @return A full detailed message from the exception.
   */
  public static @NotNull String getFullExceptionMessage(Throwable exception) {
    if (exception == null) {
      return "No exception provided.";
    }
    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("Exception: ").append(exception).append("\n");
    messageBuilder.append("Location: ").append(getExceptionLocation(exception)).append("\n");
    messageBuilder.append("Stack Trace:\n");
    for (StackTraceElement element : exception.getStackTrace()) {
      messageBuilder.append("\tat ").append(element.toString()).append("\n");
    }
    Throwable cause = exception.getCause();
    int depth = 0;
    while (cause != null && depth < 8) {
      messageBuilder.append("Caused by: ").append(cause).append("\n");
      for (StackTraceElement element : cause.getStackTrace()) {
        messageBuilder.append("\tat ").append(element.toString()).append("\n");
      }
      cause = cause.getCause();
      depth++;
    }
    return messageBuilder.toString();
  }

  private FlixelExceptionUtil() {}
}
