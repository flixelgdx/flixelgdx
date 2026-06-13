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
 * Optional sink for a single structured log line to the host console. Used on platforms where
 * {@code System.out} is not appropriate or where ANSI colors from the default path do not render
 * (for example, browser devtools with styled {@code console.log}).
 *
 * <p>Register with {@link org.flixelgdx.Flixel#setLogConsoleSink(FlixelLogConsoleSink) Flixel.setLogConsoleSink(FlixelLogConsoleSink)} before
 * {@link org.flixelgdx.Flixel#initialize Flixel.initialize} from the platform launcher. When set, the logger calls
 * this instead of writing ANSI text to standard output; file logging and in-game log listeners are unchanged.
 */
@FunctionalInterface
public interface FlixelLogConsoleSink {

  /**
   * Emits one log line. Arguments mirror the data used to build the normal console and file lines, without
   * ANSI codes.
   *
   * @param level The log level.
   * @param tag The tag, never {@code null} but may be empty.
   * @param message The message body.
   * @param simpleLocation In simple log mode, the location prefix such as {@code mypkg/MyClass.java:42:}; when
   *   the call site is unknown, a placeholder (for example, {@code unknown:0:}).
   * @param detailedFile Short file/line ID used in detailed mode, such as {@code MyClass.java:42}.
   * @param methodLabel Method name with parentheses, for example, {@code update()}.
   * @param timestamp The formatted timestamp used in detailed mode.
   * @param detailed {@code true} if {@link FlixelLogMode#DETAILED} formatting applies; otherwise simple mode.
   */
  void emit(
      FlixelLogLevel level,
      String tag,
      String message,
      String simpleLocation,
      String detailedFile,
      String methodLabel,
      String timestamp,
      boolean detailed);
}
