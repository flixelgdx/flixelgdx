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

import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelPlainConsoleSink;
import org.flixelgdx.util.FlixelAsciiCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelAnsiConsoleSinkTest {

  private static final String LOCATION = "org/flixelgdx/backend/desktop/logging/FlixelAnsiConsoleSinkTest.java:";

  private PrintStream originalOut;
  private ByteArrayOutputStream captured;

  @BeforeEach
  void setUp() {
    originalOut = System.out;
    captured = new ByteArrayOutputStream();
    System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
  }

  @Test
  void disabledColorsMatchThePlainSinkInSimpleMode() {
    assertPlainMatch(FlixelLogMode.SIMPLE);
  }

  @Test
  void disabledColorsMatchThePlainSinkInDetailedMode() {
    assertPlainMatch(FlixelLogMode.DETAILED);
  }

  @Test
  void simpleInfoUsesBoldLocationAndItalicMessage() {
    FlixelDefaultLogger logger = newLogger(FlixelLogMode.SIMPLE, true);
    logger.info("hello {}", 5);
    String out = output();
    assertTrue(out.startsWith(FlixelAsciiCodes.BOLD + FlixelAsciiCodes.WHITE + LOCATION), out);
    assertTrue(out.contains(FlixelAsciiCodes.RESET + " "), out);
    assertTrue(out.contains(FlixelAsciiCodes.ITALIC + FlixelAsciiCodes.WHITE + "hello 5" + FlixelAsciiCodes.RESET),
        out);
    assertFalse(out.contains(FlixelAsciiCodes.UNDERLINE), out);
  }

  @Test
  void simpleErrorUnderlinesTheLocationInRed() {
    FlixelDefaultLogger logger = newLogger(FlixelLogMode.SIMPLE, true);
    logger.error("broken");
    String out = output();
    assertTrue(out.startsWith(
        FlixelAsciiCodes.BOLD + FlixelAsciiCodes.UNDERLINE + FlixelAsciiCodes.RED + LOCATION), out);
    assertTrue(out.contains(FlixelAsciiCodes.ITALIC + FlixelAsciiCodes.RED + "broken" + FlixelAsciiCodes.RESET), out);
  }

  @Test
  void levelsUseTheirColors() {
    FlixelDefaultLogger logger = newLogger(FlixelLogMode.SIMPLE, true);
    logger.warn("w");
    assertTrue(output().contains(FlixelAsciiCodes.YELLOW + "w"));
    captured.reset();
    logger.debug("d");
    assertTrue(output().contains(FlixelAsciiCodes.BLUE + "d"));
  }

  @Test
  void detailedModeStylesEachPart() {
    FlixelDefaultLogger logger = newLogger(FlixelLogMode.DETAILED, true);
    logger.setTag("Tag");
    logger.warn("careful");
    String out = output();
    String yellow = FlixelAsciiCodes.YELLOW;
    assertTrue(out.startsWith(yellow), out);
    assertTrue(out.contains(FlixelAsciiCodes.BOLD + yellow + "[WARN] "), out);
    assertTrue(out.contains(FlixelAsciiCodes.BOLD + yellow + "[Tag] "), out);
    assertTrue(out.contains(FlixelAsciiCodes.BOLD + yellow + "[FlixelAnsiConsoleSinkTest.java:"), out);
    assertTrue(out.contains(yellow + "[detailedModeStylesEachPart()]" + FlixelAsciiCodes.RESET), out);
    assertTrue(out.endsWith(
        FlixelAsciiCodes.ITALIC + yellow + " careful" + FlixelAsciiCodes.RESET + System.lineSeparator()), out);
  }

  @Test
  void throwableTraceFollowsTheMessageInTheLevelColor() {
    FlixelDefaultLogger logger = newLogger(FlixelLogMode.SIMPLE, true);
    logger.error("failed", new IllegalStateException("boom"));
    String out = output();
    int messageEnd = out.indexOf("failed" + FlixelAsciiCodes.RESET);
    assertTrue(messageEnd > 0, out);
    String trace = out.substring(messageEnd);
    assertTrue(trace.contains(FlixelAsciiCodes.RED + "\n"), out);
    assertTrue(trace.contains("IllegalStateException"), out);
    assertTrue(trace.contains("boom"), out);
  }

  @Test
  void colorToggleIsReadable() {
    FlixelAnsiConsoleSink sink = new FlixelAnsiConsoleSink(false);
    assertFalse(sink.isColorEnabled());
    sink.setColorEnabled(true);
    assertTrue(sink.isColorEnabled());
  }

  private void assertPlainMatch(FlixelLogMode mode) {
    FlixelDefaultLogger logger = new FlixelDefaultLogger(mode);
    logger.setConsoleSink(null);
    logger.setSiteResolver(new FlixelJvmLogSiteResolver());
    logger.setTag("T");
    logger.addExtraSink(new FlixelPlainConsoleSink());
    logger.addExtraSink(new FlixelAnsiConsoleSink(false));
    logger.info("plain {}", 1);
    assertTwinOutput();
    logger.error("with trace", new IllegalArgumentException("oops"));
    assertTwinOutput();
  }

  /**
   * Both sinks receive every message, so each message prints twice in a row: first from the plain
   * sink, then from the ANSI sink with colors off. The two halves must be identical.
   */
  private void assertTwinOutput() {
    String out = output();
    captured.reset();
    assertFalse(out.contains("\u001B["), out);
    assertEquals(0, out.length() % 2, out);
    int half = out.length() / 2;
    assertTrue(half > 0);
    assertEquals(out.substring(0, half), out.substring(half));
  }

  private FlixelDefaultLogger newLogger(FlixelLogMode mode, boolean color) {
    FlixelDefaultLogger logger = new FlixelDefaultLogger(mode);
    logger.setConsoleSink(new FlixelAnsiConsoleSink(color));
    logger.setSiteResolver(new FlixelJvmLogSiteResolver());
    return logger;
  }

  private String output() {
    return captured.toString(StandardCharsets.UTF_8);
  }
}
