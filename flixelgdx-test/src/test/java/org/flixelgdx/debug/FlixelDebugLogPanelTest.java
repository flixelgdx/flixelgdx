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
package org.flixelgdx.debug;

import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugLogPanelTest {

  private FlixelDebugLogPanel logs;
  private FlixelLogger log;

  @BeforeEach
  void setUp() {
    logs = new FlixelDebugLogPanel();
    FlixelDefaultLogger root = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    root.setConsoleSink(null);
    root.addSink(logs.getSink());
    log = root.tagged("Test");
  }

  private record Line(long seq, double time, int level, boolean uncaught, String tag, String site, String message,
      String stack) {
  }

  private static Line readLine(FlixelDebugMessageReader r) {
    return new Line(r.readI64(), r.readF64(), r.readU8(), r.readBool(), DebugTestSupport.str(r),
        DebugTestSupport.str(r), DebugTestSupport.str(r), DebugTestSupport.str(r));
  }

  @Test
  void writeEncodesNewLinesWithLevelsTagAndSequenceNumbers() {
    log.debug("d");
    log.info("i");
    log.warn("w");
    log.error("e");

    FlixelDebugMessageReader r = DebugTestSupport.payload(logs);
    assertFalse(r.readBool(), "backlog");
    assertEquals(4, r.readCount());
    int[] levels = { FlixelDebugProtocol.LEVEL_DEBUG, FlixelDebugProtocol.LEVEL_INFO,
        FlixelDebugProtocol.LEVEL_WARN, FlixelDebugProtocol.LEVEL_ERROR };
    String[] messages = { "d", "i", "w", "e" };
    long previous = 0;
    for (int i = 0; i < 4; i++) {
      Line line = readLine(r);
      assertEquals(levels[i], line.level());
      assertEquals(messages[i], line.message());
      assertEquals("Test", line.tag());
      assertFalse(line.uncaught());
      assertEquals("", line.stack());
      assertTrue(line.seq() > previous);
      assertTrue(line.time() >= 0.0);
      previous = line.seq();
    }
    assertEquals(0, r.getRemaining());
  }

  @Test
  void aSecondWriteOnlyHoldsLinesThatArrivedSinceTheFirst() {
    log.info("one");
    log.info("two");
    assertTrue(logs.hasNewLines());
    DebugTestSupport.payload(logs);
    assertFalse(logs.hasNewLines());

    FlixelDebugMessageReader empty = DebugTestSupport.payload(logs);
    empty.readBool();
    assertEquals(0, empty.readCount());

    log.info("three");
    assertTrue(logs.hasNewLines());
    FlixelDebugMessageReader r = DebugTestSupport.payload(logs);
    r.readBool();
    assertEquals(1, r.readCount());
    assertEquals("three", readLine(r).message());
  }

  @Test
  void theBacklogWriteHoldsEverythingWithTheBacklogFlag() {
    log.info("one");
    log.info("two");
    DebugTestSupport.payload(logs);
    log.info("three");

    FlixelDebugMessageReader r = DebugTestSupport.payload(new BacklogView(logs));
    assertTrue(r.readBool(), "backlog");
    assertEquals(3, r.readCount());
    assertEquals("one", readLine(r).message());
    assertEquals("two", readLine(r).message());
    assertEquals("three", readLine(r).message());
    assertFalse(logs.hasNewLines(), "a backlog write counts as sent");
  }

  @Test
  void theRingKeepsTheNewest500Lines() {
    for (int i = 1; i <= 600; i++) {
      log.info("line {}", i);
    }
    assertEquals(FlixelDebugLogPanel.MAX_LINES, logs.getLineCount());
    assertEquals(600, logs.getNewestSeq());

    FlixelDebugMessageReader r = DebugTestSupport.payload(logs);
    r.readBool();
    assertEquals(500, r.readCount());
    Line first = readLine(r);
    assertEquals(101, first.seq());
    assertEquals("line 101", first.message());
    Line last = null;
    for (int i = 1; i < 500; i++) {
      last = readLine(r);
    }
    assertEquals(600, last.seq());
    assertEquals("line 600", last.message());
  }

  @Test
  void longMessagesAreCutAt2000CharactersWithAMarker() {
    log.info("x".repeat(2000));
    log.info("y".repeat(2500));

    FlixelDebugMessageReader r = DebugTestSupport.payload(logs);
    r.readBool();
    assertEquals(2, r.readCount());
    assertEquals("x".repeat(2000), readLine(r).message());
    assertEquals("y".repeat(2000) + " (cut off)", readLine(r).message());
  }

  @Test
  void aThrowableFillsTheStackField() {
    Exception cause = new IllegalArgumentException("root cause");
    log.error("failed", new IllegalStateException("outer", cause));

    FlixelDebugMessageReader r = DebugTestSupport.payload(logs);
    r.readBool();
    r.readCount();
    Line line = readLine(r);
    assertEquals("failed", line.message());
    assertTrue(line.stack().startsWith(IllegalStateException.class.getName() + ": outer\n"), line.stack());
    assertTrue(line.stack().contains("\tat "), line.stack());
    assertTrue(line.stack().contains("Caused by: " + IllegalArgumentException.class.getName() + ": root cause"),
        line.stack());
    assertTrue(line.stack().contains("FlixelDebugLogPanelTest"));
  }

  @Test
  void uncaughtExceptionMessagesAreFlagged() {
    log.error("There was an uncaught exception on thread \"main\"!", new RuntimeException("x"));
    log.error("an ordinary error");

    FlixelDebugMessageReader r = DebugTestSupport.payload(logs);
    r.readBool();
    r.readCount();
    assertTrue(readLine(r).uncaught());
    assertFalse(readLine(r).uncaught());
  }

  @Test
  void theClearActionEmptiesTheBuffer() {
    log.info("one");
    log.info("two");
    DebugTestSupport.send(logs, FlixelDebugProtocol.LOGS_CLEAR, w -> {
    });
    assertEquals(0, logs.getLineCount());
    assertFalse(logs.hasNewLines());

    log.info("three");
    FlixelDebugMessageReader r = DebugTestSupport.payload(new BacklogView(logs));
    r.readBool();
    assertEquals(1, r.readCount());
    assertEquals(3, readLine(r).seq(), "sequence numbers keep counting after a clear");
  }

  @Test
  void unknownActionsAreIgnored() {
    log.info("one");
    DebugTestSupport.send(logs, 77, w -> {
    });
    assertEquals(1, logs.getLineCount());
  }

  @Test
  void theRevisionChangesWithEveryLine() {
    long before = logs.getRevision();
    log.info("one");
    assertTrue(logs.getRevision() > before);
  }

  private static final class BacklogView extends FlixelDebugPanelBase {

    private final FlixelDebugLogPanel logs;

    BacklogView(FlixelDebugLogPanel logs) {
      super("view", "View", FlixelDebugProtocol.KIND_LOGS);
      this.logs = logs;
    }

    @Override
    public void write(FlixelDebugMessageWriter writer) {
      logs.writeBacklog(writer);
    }
  }
}
