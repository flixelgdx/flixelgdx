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

import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;

/**
 * The default console sink: prints each message as plain text to standard output, with no colors.
 *
 * <p>Plain text is safe everywhere (piped output, CI logs, browser consoles that do not understand
 * escape codes). Platform backends replace this sink with a nicer one, such as a colored terminal
 * sink on desktop.
 *
 * <p>The line layout follows the mode that the logger had when the message was logged: see
 * {@link FlixelLogFormat#appendSimple(FlixelString, FlixelLogEntry)} and
 * {@link FlixelLogFormat#appendDetailed(FlixelString, FlixelLogEntry)}. When the message carries an
 * exception, its stack trace is printed on the lines after the message.
 */
public class FlixelPlainConsoleSink implements FlixelLogSink {

  private final FlixelString line = new FlixelString(512);

  @Override
  public void write(@NotNull FlixelLogEntry entry) {
    line.clear();
    if (entry.getMode() == FlixelLogMode.DETAILED) {
      FlixelLogFormat.appendDetailed(line, entry);
    } else {
      FlixelLogFormat.appendSimple(line, entry);
    }
    Throwable t = entry.getThrowable();
    if (t != null) {
      FlixelLogFormat.appendThrowable(line, t);
    }
    System.out.println(line);
  }
}
