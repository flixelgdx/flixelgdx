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
package org.flixelgdx.backend.html5.logging;

import org.flixelgdx.logging.FlixelLogEntry;
import org.flixelgdx.logging.FlixelLogFormat;
import org.flixelgdx.logging.FlixelLogLevel;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogSink;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.JSBody;

/**
 * A console sink that prints each log message to the browser's developer tools console.
 *
 * <p>Every level goes to the console method with the same name ({@code console.debug},
 * {@code console.info}, {@code console.warn}, and {@code console.error}), so the level filters in the
 * developer tools (the "Verbose", "Info", "Warnings", and "Errors" checkboxes) work on FlixelGDX
 * messages just like they do for any other web page.
 *
 * <p>Think of the developer tools console as a mailroom with a separate slot for each kind of mail:
 * putting a message in the right slot lets the staff sort it later without reading every letter.
 *
 * <p>When styling is on (the default), the location part of the line is drawn in bold and a debug
 * message is drawn in blue. The colors were picked to stay readable on both light and dark developer
 * tools themes. Warnings and errors keep the browser's own highlighting, so they are not recolored.
 * Turn styling off with {@link #setStyled(boolean)} for a plain text line.
 *
 * <p>The text layout comes from {@link FlixelLogFormat}, the same as every other sink, and the
 * message is passed to the console as a separate argument instead of being glued into the format
 * string, so a {@code %} inside a message is never mistaken for a console format directive.
 *
 * <p>The script that talks to the console is a plain {@link JSBody}, which behaves the same on both
 * the JavaScript and the WebAssembly GC targets of TeaVM.
 *
 * <p>Example (the launcher already does this for you):
 * <pre>{@code
 * Flixel.log.setConsoleSink(new FlixelHtml5ConsoleSink());
 * }</pre>
 */
public class FlixelHtml5ConsoleSink implements FlixelLogSink {

  private final FlixelString line = new FlixelString(512);

  private boolean styled = true;

  @Override
  public void write(@NotNull FlixelLogEntry entry) {
    line.clear();
    if (entry.getMode() == FlixelLogMode.DETAILED) {
      FlixelLogFormat.appendDetailed(line, entry);
    } else {
      FlixelLogFormat.appendSimple(line, entry);
    }
    // Both layouts end with the message text, so everything before it is the location part. The
    // script splits the one string at that point, which avoids creating a second Java string.
    int headLen = line.length() - entry.getMessage().length();
    Throwable t = entry.getThrowable();
    if (t != null) {
      FlixelLogFormat.appendThrowable(line, t);
    }
    print(levelCode(entry.getLevel()), line.toString(), headLen, styled);
  }

  private static int levelCode(FlixelLogLevel level) {
    return switch (level) {
      case DEBUG -> 0;
      case INFO -> 1;
      case WARN -> 2;
      case ERROR -> 3;
    };
  }

  /**
   * Returns whether the location part of each line is styled with CSS.
   *
   * @return {@code true} when styled (the default), {@code false} for plain text.
   */
  public boolean isStyled() {
    return styled;
  }

  /**
   * Turns the CSS styling of each line on or off.
   *
   * @param styled {@code true} for a bold location and a blue debug message, {@code false} for
   *   plain text.
   */
  public void setStyled(boolean styled) {
    this.styled = styled;
  }

  /**
   * Sends one line to the matching developer tools console method.
   *
   * @param level The level code: 0 for debug, 1 for info, 2 for warn, and 3 for error.
   * @param text The whole line, where the first {@code headLen} characters are the location part.
   * @param headLen How many characters at the start of {@code text} belong to the location part.
   * @param styled Whether to apply the CSS styling.
   */
  @JSBody(params = {"level", "text", "headLen", "styled"}, script = """
      var flixelFn = level === 0 ? console.debug
        : level === 1 ? console.info
        : level === 2 ? console.warn
        : console.error;
      var flixelHead = text.substring(0, headLen);
      var flixelBody = text.substring(headLen);
      if (styled) {
        flixelFn.call(console, '%c%s%c%s', 'font-weight:bold', flixelHead,
          level === 0 ? 'color:#4a90e2' : '', flixelBody);
      } else {
        flixelFn.call(console, '%s%s', flixelHead, flixelBody);
      }
      """)
  private static native void print(int level, String text, int headLen, boolean styled);
}
