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
package org.flixelgdx.backend.desktop.logging;

import org.flixelgdx.logging.FlixelLogEntry;
import org.flixelgdx.logging.FlixelLogFormat;
import org.flixelgdx.logging.FlixelLogLevel;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogSink;
import org.flixelgdx.logging.FlixelLogSite;
import org.flixelgdx.logging.FlixelPlainConsoleSink;
import org.flixelgdx.util.FlixelAsciiCodes;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;

/**
 * A console sink that prints each log message with ANSI colors and text styles.
 *
 * <p>Each level has its own color: INFO is white, WARN is yellow, ERROR is red, and DEBUG is blue. In
 * the simple layout the location prefix is bold (and underlined for errors) and the message is italic.
 * In the detailed layout the timestamp, level, tag, file, and method are colored the same way, with the
 * message in italics. When the message carries an exception, its stack trace is printed after the
 * message in the level color.
 *
 * <p>Colors are on by default. They turn off when the {@code NO_COLOR} environment variable is set to
 * any non-empty value, or when the system property {@code flixel.log.color} is {@code false}. They can
 * also be switched at any time with {@link #setColorEnabled(boolean)}. With colors off, the output is
 * exactly what {@link FlixelPlainConsoleSink} prints, which is handy for piped output and CI logs.
 *
 * <p>The line is built in a reused buffer and printed with a single {@code System.out.println(...)}
 * call, so a message is never split up by output from other threads.
 *
 * <p>Install it from the desktop launcher:
 * <pre>{@code
 * Flixel.log.setConsoleSink(new FlixelAnsiConsoleSink());
 * }</pre>
 */
public class FlixelAnsiConsoleSink implements FlixelLogSink {

  private final FlixelString line = new FlixelString(512);
  private final FlixelString scratch = new FlixelString(256);

  private volatile boolean colorEnabled;

  /** Creates a sink whose colors follow {@code NO_COLOR} and {@code flixel.log.color}. */
  public FlixelAnsiConsoleSink() {
    this(detectColorSupport());
  }

  /**
   * Creates a sink with colors explicitly on or off.
   *
   * @param colorEnabled Whether to print ANSI color codes.
   */
  public FlixelAnsiConsoleSink(boolean colorEnabled) {
    this.colorEnabled = colorEnabled;
  }

  @Override
  public void write(@NotNull FlixelLogEntry entry) {
    line.clear();
    Throwable t = entry.getThrowable();
    boolean detailed = entry.getMode() == FlixelLogMode.DETAILED;
    if (!colorEnabled) {
      if (detailed) {
        FlixelLogFormat.appendDetailed(line, entry);
      } else {
        FlixelLogFormat.appendSimple(line, entry);
      }
      if (t != null) {
        FlixelLogFormat.appendThrowable(line, t);
      }
      System.out.println(line);
      return;
    }

    FlixelLogLevel level = entry.getLevel();
    String color = colorFor(level);
    boolean underline = level == FlixelLogLevel.ERROR;
    if (detailed) {
      appendDetailedColored(entry, color, underline);
    } else {
      appendSimpleColored(entry, color, underline);
    }
    if (t != null) {
      line.concat(color);
      FlixelLogFormat.appendThrowable(line, t);
      line.concat(FlixelAsciiCodes.RESET);
    }
    System.out.println(line);
  }

  private void appendSimpleColored(FlixelLogEntry entry, String color, boolean underline) {
    // The plain simple layout is "location: message". Format it once, then cut off the location
    // part so the location and the message can be styled separately.
    scratch.clear();
    FlixelLogFormat.appendSimple(scratch, entry);
    int locationEnd = scratch.length() - entry.getMessage().length() - 1;
    open(color, true, false, underline);
    for (int i = 0; i < locationEnd; i++) {
      line.concat(scratch.charAt(i));
    }
    line.concat(FlixelAsciiCodes.RESET);
    line.concat(' ');
    open(color, false, true, false);
    line.concat(entry.getMessage());
    line.concat(FlixelAsciiCodes.RESET);
  }

  private void appendDetailedColored(FlixelLogEntry entry, String color, boolean underline) {
    FlixelLogSite site = entry.site();
    open(color, false, false, underline);
    FlixelLogFormat.appendTimestamp(line, entry.getTime());
    line.concat(' ');
    line.concat(FlixelAsciiCodes.RESET);

    open(color, true, false, underline);
    line.concat('[');
    line.concat(entry.getLevel().name());
    line.concat(']');
    line.concat(' ');
    line.concat(FlixelAsciiCodes.RESET);

    open(color, true, false, underline);
    line.concat('[');
    line.concat(entry.getTag());
    line.concat(']');
    line.concat(' ');
    line.concat(FlixelAsciiCodes.RESET);

    open(color, true, false, underline);
    line.concat('[');
    line.concat(site.getFileName());
    line.concat(':');
    line.concat(site.getLine());
    line.concat(']');
    line.concat(' ');
    line.concat(FlixelAsciiCodes.RESET);

    open(color, false, false, underline);
    line.concat('[');
    line.concat(site.getMethodName());
    line.concat('(');
    line.concat(')');
    line.concat(']');
    line.concat(FlixelAsciiCodes.RESET);

    open(color, false, true, false);
    line.concat(' ');
    line.concat(entry.getMessage());
    line.concat(FlixelAsciiCodes.RESET);
  }

  private void open(String color, boolean bold, boolean italic, boolean underline) {
    if (bold) {
      line.concat(FlixelAsciiCodes.BOLD);
    }
    if (italic) {
      line.concat(FlixelAsciiCodes.ITALIC);
    }
    if (underline) {
      line.concat(FlixelAsciiCodes.UNDERLINE);
    }
    line.concat(color);
  }

  private static String colorFor(FlixelLogLevel level) {
    return switch (level) {
      case INFO -> FlixelAsciiCodes.WHITE;
      case WARN -> FlixelAsciiCodes.YELLOW;
      case ERROR -> FlixelAsciiCodes.RED;
      case DEBUG -> FlixelAsciiCodes.BLUE;
    };
  }

  private static boolean detectColorSupport() {
    String noColor = System.getenv("NO_COLOR");
    if (noColor != null && !noColor.isEmpty()) {
      return false;
    }
    return !"false".equalsIgnoreCase(System.getProperty("flixel.log.color"));
  }

  public boolean isColorEnabled() {
    return colorEnabled;
  }

  public void setColorEnabled(boolean colorEnabled) {
    this.colorEnabled = colorEnabled;
  }
}
