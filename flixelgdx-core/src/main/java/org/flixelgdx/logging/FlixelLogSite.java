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
 * A mutable holder that describes where in the source code a log call was made.
 *
 * <p>A single instance is reused by each {@link FlixelLogEntry}, and a {@link FlixelLogSiteResolver}
 * fills it in. Like the entry itself, it is only valid during the sink call, so copy the values you
 * want to keep.
 */
public final class FlixelLogSite {

  private int line;
  private String fileName = "Unknown";
  private String className = "";
  private String methodName = "unknown";

  /**
   * Sets every field at once.
   *
   * @param fileName The source file name, such as {@code MyState.java}. A {@code null} value becomes
   *   {@code "Unknown"}.
   * @param line The source line number, or {@code 0} if unknown. Negative values become {@code 0}.
   * @param className The fully qualified name of the class that made the call. A {@code null} value
   *   becomes an empty string.
   * @param methodName The simple name of the method that made the call, without parentheses. A
   *   {@code null} value becomes {@code "unknown"}.
   */
  public void set(String fileName, int line, String className, String methodName) {
    this.fileName = fileName != null ? fileName : "Unknown";
    this.line = Math.max(line, 0);
    this.className = className != null ? className : "";
    this.methodName = methodName != null ? methodName : "unknown";
  }

  /** Resets every field to the values that mean "unknown site". */
  public void clear() {
    set(null, 0, null, null);
  }

  /**
   * Returns the source file name.
   *
   * @return The file name, for example {@code MyState.java}, never {@code null}.
   */
  @NotNull
  public String getFileName() {
    return fileName;
  }

  /**
   * Returns the source line number.
   *
   * @return The line number, or {@code 0} if unknown.
   */
  public int getLine() {
    return line;
  }

  /**
   * Returns the fully qualified class name.
   *
   * @return The class name, or an empty string if unknown, never {@code null}.
   */
  @NotNull
  public String getClassName() {
    return className;
  }

  /**
   * Returns the simple method name, without parentheses.
   *
   * @return The method name, never {@code null}.
   */
  @NotNull
  public String getMethodName() {
    return methodName;
  }
}
