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

import org.flixelgdx.GdxHeadlessExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(GdxHeadlessExtension.class)
class FlixelLoggerTest {

  private FlixelLogger logger;
  private List<FlixelLogEntry> captured;
  private java.util.function.Consumer<FlixelLogEntry> captureListener;

  @BeforeEach
  void setUp() {
    logger = new FlixelLogger(FlixelLogMode.SIMPLE);
    logger.setStackTraceProvider(null);
    captured = new ArrayList<>();
    captureListener = captured::add;
    logger.addLogListener(captureListener);
  }

  // -- Listener receives correct level and message --

  @Test
  void debugCallNotifiesListenerWithDebugLevel() {
    logger.debug("hello");
    assertEquals(1, captured.size());
    assertEquals(FlixelLogLevel.DEBUG, captured.get(0).level());
    assertEquals("hello", captured.get(0).message());
  }

  @Test
  void infoCallNotifiesListenerWithInfoLevel() {
    logger.info("status update");
    assertEquals(FlixelLogLevel.INFO, captured.get(0).level());
    assertEquals("status update", captured.get(0).message());
  }

  @Test
  void warnCallNotifiesListenerWithWarnLevel() {
    logger.warn("low memory");
    assertEquals(FlixelLogLevel.WARN, captured.get(0).level());
  }

  @Test
  void errorCallNotifiesListenerWithErrorLevel() {
    logger.error("explosion");
    assertEquals(FlixelLogLevel.ERROR, captured.get(0).level());
    assertEquals("explosion", captured.get(0).message());
  }

  // -- Tag routing --

  @Test
  void taggedDebugSetsTagOnEntry() {
    logger.debug("Physics", "body count: 5");
    assertEquals("Physics", captured.get(0).tag());
    assertEquals("body count: 5", captured.get(0).message());
  }

  @Test
  void defaultTagIsUsedWhenNoTagProvided() {
    logger.setDefaultTag("Game");
    logger.info("running");
    assertEquals("Game", captured.get(0).tag());
  }

  @Test
  void setDefaultTagNullResetsToEmptyString() {
    logger.setDefaultTag(null);
    assertEquals("", logger.getDefaultTag());
  }

  // -- Format string interpolation --

  @Test
  void singlePlaceholderIsReplaced() {
    logger.debug("Tag", "loaded {} assets", 42);
    assertEquals("loaded 42 assets", captured.get(0).message());
  }

  @Test
  void multiplePlaceholdersAreReplacedInOrder() {
    logger.info("Tag", "pos {},{}", 10, 20);
    assertEquals("pos 10,20", captured.get(0).message());
  }

  @Test
  void excessPlaceholdersAreLeftAsIs() {
    logger.warn("Tag", "a {} b {} c {}", "x");
    assertEquals("a x b {} c {}", captured.get(0).message());
  }

  @Test
  void nullMessageIsRenderedAsLiteralNull() {
    logger.debug(null);
    assertEquals("null", captured.get(0).message());
  }

  // -- Error with throwable --

  @Test
  void errorWithThrowableAppendsMsExceptionSuffix() {
    RuntimeException ex = new RuntimeException("boom");
    logger.error((Object) "oops", ex);
    assertTrue(captured.get(0).message().contains("| Exception:"), "message should include exception suffix");
    assertTrue(captured.get(0).message().startsWith("oops"));
  }

  // -- Listener add / remove --

  @Test
  void removeListenerStopsNotifications() {
    logger.removeLogListener(captureListener);
    logger.debug("silent");
    assertTrue(captured.isEmpty());
  }

  @Test
  void addNullListenerDoesNotThrow() {
    logger.addLogListener(null);
    logger.debug("ok");
  }

  // -- Log mode getter / setter --

  @Test
  void getLogModeReturnsCurrentMode() {
    assertEquals(FlixelLogMode.SIMPLE, logger.getLogMode());
  }

  @Test
  void setLogModeNullDefaultsToSimple() {
    logger.setLogMode(null);
    assertEquals(FlixelLogMode.SIMPLE, logger.getLogMode());
  }

  @Test
  void setLogModeChangesMode() {
    logger.setLogMode(FlixelLogMode.DETAILED);
    assertEquals(FlixelLogMode.DETAILED, logger.getLogMode());
  }

  // -- Logs folder --

  @Test
  void setLogsFolderWithNullClearsPath() {
    logger.setLogsFolder(null);
    assertNull(logger.getLogsFolder());
  }

  @Test
  void setLogsFolderWithEmptyStringClearsPath() {
    logger.setLogsFolder("");
    assertNull(logger.getLogsFolder());
  }

  @Test
  void setLogsFolderStripsTrailingSlash() {
    logger.setLogsFolder("/var/log/game/");
    assertEquals("/var/log/game", logger.getLogsFolder());
  }

  @Test
  void setLogsFolderWithNormalPathIsPreserved() {
    logger.setLogsFolder("/var/log/game");
    assertEquals("/var/log/game", logger.getLogsFolder());
  }
}
