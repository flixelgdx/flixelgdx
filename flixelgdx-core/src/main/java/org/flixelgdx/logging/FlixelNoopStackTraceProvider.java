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

/**
 * Default {@link FlixelStackTraceProvider} used when no platform provider is installed.
 *
 * <p>Every caller lookup reports an unknown location, so log lines simply omit the
 * file-and-line suffix instead of crashing. Platforms with real stack access (the JVM module)
 * install a proper provider before startup.
 */
public enum FlixelNoopStackTraceProvider implements FlixelStackTraceProvider {

  /** Shared no-op instance. */
  INSTANCE;

  private static final FlixelStackFrame UNKNOWN = new FlixelStackFrame() {

    @Override
    public String getFileName() {
      return "Unknown";
    }

    @Override
    public int getLineNumber() {
      return 0;
    }

    @Override
    public String getClassName() {
      return "Unknown";
    }

    @Override
    public String getMethodName() {
      return "Unknown";
    }
  };

  @Override
  public FlixelStackFrame getCaller() {
    return UNKNOWN;
  }
}
