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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelLoggerTest {

  private FlixelDefaultLogger logger;
  private List<Captured> captured;
  private FlixelLogSink capture;

  @BeforeEach
  void setUp() {
    logger = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    logger.setConsoleSink(null);
    captured = new ArrayList<>();
    capture = entry -> captured.add(new Captured(entry));
    logger.addExtraSink(capture);
  }

  @Test
  void debugInfoWarnErrorCarryTheirLevel() {
    logger.debug("a");
    logger.info("b");
    logger.warn("c");
    logger.error("d");
    assertEquals(4, captured.size());
    assertEquals(FlixelLogLevel.DEBUG, captured.get(0).level);
    assertEquals(FlixelLogLevel.INFO, captured.get(1).level);
    assertEquals(FlixelLogLevel.WARN, captured.get(2).level);
    assertEquals(FlixelLogLevel.ERROR, captured.get(3).level);
    assertEquals("d", captured.get(3).message);
  }

  @Test
  void placeholdersAreFilledInOrder() {
    logger.info("{} and {} and {}", 1, "two", 3.5);
    assertEquals("1 and two and 3.5", captured.get(0).message);
  }

  @Test
  void singleArgumentAndNumericFormatsMatchToString() {
    logger.info("{}|{}|{}", 7L, 1.5f, 'c');
    assertEquals("7|1.5|c", captured.get(0).message);
    logger.info("{}|{}|{}", true, (short) 3, (byte) 4);
    assertEquals("true|3|4", captured.get(1).message);
    logger.info("{}", new StringBuilder("sb"));
    assertEquals("sb", captured.get(2).message);
  }

  @Test
  void warnWithNumericArgumentFormatsInsteadOfBeingTreatedAsATag() {
    logger.warn("x {}", 3);
    assertEquals("x 3", captured.get(0).message);
    assertEquals(FlixelLogLevel.WARN, captured.get(0).level);
  }

  @Test
  void fewerArgumentsThanPlaceholdersLeavesLiteralBraces() {
    logger.info("{} then {}", "a");
    assertEquals("a then {}", captured.get(0).message);
  }

  @Test
  void moreArgumentsThanPlaceholdersAreAppendedWithASpace() {
    logger.info("a {}", 1, 2, 3);
    assertEquals("a 1 2 3", captured.get(0).message);
  }

  @Test
  void argumentsWithoutPlaceholdersAreAppended() {
    logger.info("AI", "msg");
    assertEquals("AI msg", captured.get(0).message);
    logger.info("one", "two", "three");
    assertEquals("one two three", captured.get(1).message);
  }

  @Test
  void varargsOverloadFormatsLikeTheFixedOverloads() {
    logger.info("{} {} {} {}", 1, 2, 3, 4);
    assertEquals("1 2 3 4", captured.get(0).message);
    logger.info("{} {} {} {} {}", 1, 2, 3, 4);
    assertEquals("1 2 3 4 {}", captured.get(1).message);
  }

  @Test
  void trailingThrowableBecomesTheEntryThrowable() {
    RuntimeException boom = new RuntimeException("boom");
    logger.error("Failed to load slot {}", 2, boom);
    assertEquals("Failed to load slot 2", captured.get(0).message);
    assertSame(boom, captured.get(0).throwable);
  }

  @Test
  void throwableAsOnlyArgumentBecomesTheEntryThrowable() {
    RuntimeException boom = new RuntimeException("boom");
    logger.error("Save failed", boom);
    assertEquals("Save failed", captured.get(0).message);
    assertSame(boom, captured.get(0).throwable);
  }

  @Test
  void trailingThrowableInVarargsBecomesTheEntryThrowable() {
    RuntimeException boom = new RuntimeException("boom");
    logger.error("{} {} {}", 1, 2, 3, boom);
    assertEquals("1 2 3", captured.get(0).message);
    assertSame(boom, captured.get(0).throwable);
  }

  @Test
  void throwableConsumedByAPlaceholderStaysInTheMessage() {
    RuntimeException boom = new RuntimeException("boom");
    logger.error("failed: {}", boom);
    assertEquals("failed: " + boom, captured.get(0).message);
    assertNull(captured.get(0).throwable);
  }

  @Test
  void throwableInTheMiddleIsAppendedAsText() {
    RuntimeException boom = new RuntimeException("boom");
    logger.error("m", boom, "after");
    assertEquals("m " + boom + " after", captured.get(0).message);
    assertNull(captured.get(0).throwable);
  }

  @Test
  void nullMessageAndNullArgumentsPrintAsNull() {
    logger.info((Object) null);
    assertEquals("null", captured.get(0).message);
    logger.info("a {}", (Object) null);
    assertEquals("a null", captured.get(1).message);
  }

  @Test
  void nullVarargsArrayCountsAsNoArguments() {
    logger.info("a {}", (Object[]) null);
    assertEquals("a {}", captured.get(0).message);
  }

  @Test
  void nonStringMessageUsesToString() {
    logger.info(42);
    assertEquals("42", captured.get(0).message);
  }

  @Test
  void defaultLevelLogsEverything() {
    assertEquals(FlixelLogLevel.DEBUG, logger.getLevel());
    assertTrue(logger.isEnabled(FlixelLogLevel.DEBUG));
  }

  @Test
  void levelFilterUsesSeverityNotDeclarationOrder() {
    logger.setLevel(FlixelLogLevel.INFO);
    logger.debug("hidden");
    logger.info("shown 1");
    logger.warn("shown 2");
    logger.error("shown 3");
    assertEquals(3, captured.size());
    assertFalse(logger.isEnabled(FlixelLogLevel.DEBUG));
    assertTrue(logger.isEnabled(FlixelLogLevel.INFO));

    captured.clear();
    logger.setLevel(FlixelLogLevel.WARN);
    logger.debug("hidden");
    logger.info("hidden");
    logger.warn("shown");
    logger.error("shown");
    assertEquals(2, captured.size());

    captured.clear();
    logger.setLevel(FlixelLogLevel.ERROR);
    logger.warn("hidden");
    logger.error("shown");
    assertEquals(1, captured.size());
    assertEquals(FlixelLogLevel.ERROR, captured.get(0).level);
  }

  @Test
  void nullLevelAndModeFallBackToDefaults() {
    logger.setLevel(FlixelLogLevel.ERROR);
    logger.setLevel(null);
    assertEquals(FlixelLogLevel.DEBUG, logger.getLevel());
    logger.setMode(FlixelLogMode.DETAILED);
    logger.setMode(null);
    assertEquals(FlixelLogMode.SIMPLE, logger.getMode());
    assertEquals(FlixelLogMode.SIMPLE, new FlixelDefaultLogger(null).getMode());
  }

  @Test
  void filteredMessageDoesNotEvaluateArguments() {
    logger.setLevel(FlixelLogLevel.ERROR);
    Object trap = new Object() {
      @Override
      public String toString() {
        throw new AssertionError("A filtered message must not touch its arguments.");
      }
    };
    logger.debug("{}", trap);
    assertTrue(captured.isEmpty());
  }

  @Test
  void setTagControlsTheTagOfTheEntry() {
    assertEquals("", logger.getTag());
    logger.info("untagged");
    logger.setTag("MyGame");
    logger.info("tagged");
    logger.setTag(null);
    assertEquals("", logger.getTag());
    assertEquals("", captured.get(0).tag);
    assertEquals("MyGame", captured.get(1).tag);
  }

  @Test
  void taggedChildUsesItsOwnTagButSharesTheRoot() {
    logger.setTag("Root");
    FlixelLogger ai = logger.tagged("AI");
    ai.info("from child");
    logger.info("from root");
    assertEquals("AI", captured.get(0).tag);
    assertEquals("Root", captured.get(1).tag);
    assertEquals("AI", ai.getTag());

    ai.setTag("Changed");
    assertEquals("Root", logger.getTag());

    logger.setLevel(FlixelLogLevel.ERROR);
    assertEquals(FlixelLogLevel.ERROR, ai.getLevel());
    ai.info("hidden");
    assertEquals(2, captured.size());

    ai.setLevel(FlixelLogLevel.DEBUG);
    assertEquals(FlixelLogLevel.DEBUG, logger.getLevel());

    List<String> late = new ArrayList<>();
    ai.addExtraSink(entry -> late.add(entry.getMessage().toString()));
    logger.info("both");
    assertEquals(1, late.size());
    assertEquals("both", late.get(0));
  }

  @Test
  void taggedChildSharesModeAndFileSettingsWithRoot() {
    FlixelLogger child = logger.tagged("C");
    child.setMode(FlixelLogMode.DETAILED);
    child.setMaxLogFiles(3);
    child.setCanStoreLogs(false);
    child.setLogsFolder("/tmp/x/");
    assertEquals(FlixelLogMode.DETAILED, logger.getMode());
    assertEquals(3, logger.getMaxLogFiles());
    assertFalse(logger.canStoreLogs());
    assertEquals("/tmp/x", logger.getLogsFolder());
    assertEquals(FlixelLogMode.DETAILED, captureMode());
  }

  private FlixelLogMode captureMode() {
    FlixelLogMode[] seen = new FlixelLogMode[1];
    logger.addExtraSink(entry -> seen[0] = entry.getMode());
    logger.tagged("C").info("x");
    return seen[0];
  }

  @Test
  void nullTagBecomesEmpty() {
    assertEquals("", logger.tagged(null).getTag());
  }

  @Test
  void extraSinkCanBeRemovedAndNullIsIgnored() {
    logger.addExtraSink(null);
    logger.removeExtraSink(null);
    logger.info("first");
    logger.removeExtraSink(capture);
    logger.info("second");
    assertEquals(1, captured.size());
  }

  @Test
  void aThrowingSinkDoesNotStopTheOtherSinks() {
    PrintStream originalErr = System.err;
    System.setErr(new PrintStream(new ByteArrayOutputStream()));
    try {
      logger.addExtraSink(entry -> {
        throw new IllegalStateException("bad sink");
      });
      logger.addExtraSink(entry -> captured.add(new Captured(entry)));
      logger.info("hi");
    } finally {
      System.setErr(originalErr);
    }
    assertEquals(2, captured.size());
  }

  @Test
  void consoleSinkAndFileSinkReceiveMessages() {
    List<String> console = new ArrayList<>();
    logger.setConsoleSink(entry -> console.add(entry.getMessage().toString()));
    TestFileSink file = new TestFileSink();
    logger.setFileSink(file);

    logger.info("not open yet");
    assertEquals(List.of("not open yet"), console);
    assertTrue(file.lines.isEmpty());

    logger.setLogsFolder("/logs/");
    logger.setMaxLogFiles(4);
    logger.startFileLogging();
    assertEquals(1, file.opens);
    assertEquals("/logs", file.folder);
    assertEquals(4, file.max);

    logger.info("to file");
    assertEquals(List.of("to file"), file.lines);

    logger.stopFileLogging();
    assertEquals(1, file.closes);
    logger.info("after close");
    assertEquals(List.of("to file"), file.lines);
  }

  @Test
  void fileLoggingDoesNothingWhenStoringIsDisabledOrThereIsNoSink() {
    logger.startFileLogging();
    logger.stopFileLogging();

    TestFileSink file = new TestFileSink();
    logger.setFileSink(file);
    logger.setCanStoreLogs(false);
    logger.startFileLogging();
    assertEquals(0, file.opens);
  }

  @Test
  void logsFolderSetterStripsOneTrailingSlashAndTreatsEmptyAsDefault() {
    assertNull(logger.getLogsFolder());
    logger.setLogsFolder("/a/b/");
    assertEquals("/a/b", logger.getLogsFolder());
    logger.setLogsFolder("");
    assertNull(logger.getLogsFolder());
    logger.setLogsFolder(null);
    assertNull(logger.getLogsFolder());
    assertEquals(10, logger.getMaxLogFiles());
    assertTrue(logger.canStoreLogs());
  }

  @Test
  void defaultSinksAndResolverAreInstalled() {
    FlixelDefaultLogger fresh = new FlixelDefaultLogger();
    assertTrue(fresh.getConsoleSink() instanceof FlixelPlainConsoleSink);
    assertSame(FlixelLogSiteMarker.INSTANCE, fresh.getSiteResolver());
    assertNull(fresh.getFileSink());
    assertEquals(FlixelLogMode.SIMPLE, fresh.getMode());
  }

  @Test
  void entrySiteIsResolvedLazilyAndOnlyOnce() {
    int[] calls = new int[1];
    logger.setSiteResolver(out -> {
      calls[0]++;
      out.set("Game.java", 12, "org.game.Game", "update");
      return true;
    });
    String[] seen = new String[2];
    logger.addExtraSink(entry -> {
      seen[0] = entry.site().getFileName() + ":" + entry.site().getLine();
      seen[1] = entry.site().getMethodName();
    });
    logger.info("no site needed by the capture sink");
    assertEquals(1, calls[0]);
    assertEquals("Game.java:12", seen[0]);
    assertEquals("update", seen[1]);
  }

  @Test
  void entrySiteIsNotResolvedWhenNoSinkAsksForIt() {
    int[] calls = new int[1];
    logger.setSiteResolver(out -> {
      calls[0]++;
      return true;
    });
    logger.info("plain");
    assertEquals(0, calls[0]);
  }

  @Test
  void siteMarkerFeedsTheEntrySiteAndIsClearedAfterwards() {
    FlixelLogSiteMarker.clear();
    String[] seen = new String[1];
    logger.addExtraSink(entry -> seen[0] = entry.site().getFileName() + ":" + entry.site().getLine());
    FlixelLogSiteMarker.mark("Marked.java", 77, "a.b.Marked", "go");
    logger.info("with marker");
    assertEquals("Marked.java:77", seen[0]);
    assertFalse(FlixelLogSiteMarker.INSTANCE.resolve(new FlixelLogSite()));
  }

  @Test
  void siteMarkerIsClearedWhenTheMessageIsFilteredOut() {
    logger.setLevel(FlixelLogLevel.ERROR);
    FlixelLogSiteMarker.mark("Stale.java", 1, "Stale", "m");
    logger.debug("filtered");
    assertFalse(FlixelLogSiteMarker.INSTANCE.resolve(new FlixelLogSite()));
  }

  @Test
  void reentrantLogFromInsideASinkDoesNotCorruptTheOuterEntry() {
    List<String> before = new ArrayList<>();
    List<String> after = new ArrayList<>();
    FlixelDefaultLogger local = new FlixelDefaultLogger();
    local.setConsoleSink(null);
    local.addExtraSink(entry -> {
      before.add(entry.getMessage().toString());
      if (entry.getMessage().toString().equals("outer message")) {
        local.info("inner {}", 1);
      }
    });
    local.addExtraSink(entry -> after.add(entry.getMessage().toString() + "|" + entry.getTag()));
    local.setTag("T");

    PrintStream originalErr = System.err;
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    System.setErr(new PrintStream(err, true));
    try {
      local.info("outer message");
    } finally {
      System.setErr(originalErr);
    }

    assertEquals(List.of("outer message"), before);
    assertEquals(List.of("outer message|T"), after);
    String errText = err.toString().replace("\r", "");
    assertTrue(errText.contains("[INFO] [T] inner 1"), errText);
  }

  @Test
  void deeplyNestedReentrantLogsAreDroppedWithoutRecursing() {
    FlixelDefaultLogger local = new FlixelDefaultLogger();
    local.setConsoleSink(null);
    int[] depth = new int[1];
    Object bomb = new Object() {
      @Override
      public String toString() {
        depth[0]++;
        local.info("again");
        return "bomb";
      }
    };
    local.addExtraSink(entry -> local.info("{}", bomb));

    PrintStream originalErr = System.err;
    System.setErr(new PrintStream(new ByteArrayOutputStream()));
    try {
      local.info("start");
    } finally {
      System.setErr(originalErr);
    }
    assertEquals(1, depth[0]);
  }

  @Test
  void messageToStringThatLogsDoesNotCorruptTheOuterMessage() {
    Object chatty = new Object() {
      @Override
      public String toString() {
        logger.info("from toString");
        return "chatty";
      }
    };
    PrintStream originalErr = System.err;
    System.setErr(new PrintStream(new ByteArrayOutputStream()));
    try {
      logger.info("value={}", chatty);
    } finally {
      System.setErr(originalErr);
    }
    assertEquals(1, captured.size());
    assertEquals("value=chatty", captured.get(0).message);
  }

  @Test
  void manyThreadsProduceIntactLines() throws InterruptedException {
    int threads = 8;
    int perThread = 1000;
    List<String> lines = new ArrayList<>();
    logger.removeExtraSink(capture);
    logger.addExtraSink(entry -> lines.add(entry.getMessage().toString()));
    CountDownLatch start = new CountDownLatch(1);
    Thread[] workers = new Thread[threads];
    for (int t = 0; t < threads; t++) {
      int id = t;
      FlixelLogger tagged = logger.tagged("T" + id);
      workers[t] = new Thread(() -> {
        try {
          start.await();
        } catch (InterruptedException e) {
          return;
        }
        for (int i = 0; i < perThread; i++) {
          tagged.info("thread {} message {} done", id, i);
        }
      });
      workers[t].start();
    }
    start.countDown();
    for (Thread worker : workers) {
      worker.join();
    }

    assertEquals(threads * perThread, lines.size());
    int[] next = new int[threads];
    for (String line : lines) {
      String[] parts = line.split(" ");
      assertEquals(5, parts.length, line);
      assertEquals("thread", parts[0]);
      assertEquals("message", parts[2]);
      assertEquals("done", parts[4]);
      int id = Integer.parseInt(parts[1]);
      int index = Integer.parseInt(parts[3]);
      assertEquals(next[id], index, line);
      next[id]++;
    }
    for (int count : next) {
      assertEquals(perThread, count);
    }
  }

  private static final class Captured {

    final FlixelLogLevel level;
    final String tag;
    final String message;
    final Throwable throwable;

    Captured(FlixelLogEntry entry) {
      this.level = entry.getLevel();
      this.tag = entry.getTag();
      this.message = entry.getMessage().toString();
      this.throwable = entry.getThrowable();
    }
  }

  private static final class TestFileSink implements FlixelLogFileSink {

    final List<String> lines = new ArrayList<>();
    int opens;
    int closes;
    int max;
    String folder;
    boolean open;

    @Override
    public void open(String logsFolderPath, int maxLogFiles) {
      opens++;
      folder = logsFolderPath;
      max = maxLogFiles;
      open = true;
    }

    @Override
    public void close() {
      closes++;
      open = false;
    }

    @Override
    public boolean isOpen() {
      return open;
    }

    @Override
    public String getDefaultLogsFolderPath() {
      return null;
    }

    @Override
    public void write(@NotNull FlixelLogEntry entry) {
      lines.add(entry.getMessage().toString());
    }
  }
}
