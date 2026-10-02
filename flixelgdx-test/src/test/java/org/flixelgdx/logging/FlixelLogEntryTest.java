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
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelLogEntryTest {

  private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  private static FlixelLogEntry entry(FlixelLogLevel level, String tag, String message) {
    FlixelLogEntry e = new FlixelLogEntry(new FlixelString(message));
    e.reset(1_700_000_000_123L, level, FlixelLogMode.SIMPLE, tag, null, null);
    return e;
  }

  @Test
  void gettersReturnTheResetValues() {
    RuntimeException boom = new RuntimeException("boom");
    FlixelLogEntry e = new FlixelLogEntry(new FlixelString("something broke"));
    e.reset(42L, FlixelLogLevel.WARN, FlixelLogMode.DETAILED, "MyTag", boom, null);
    assertEquals(42L, e.getTime());
    assertEquals(FlixelLogLevel.WARN, e.getLevel());
    assertEquals(FlixelLogMode.DETAILED, e.getMode());
    assertEquals("MyTag", e.getTag());
    assertEquals("something broke", e.getMessage().toString());
    assertSame(boom, e.getThrowable());
  }

  @Test
  void toStringWithTagIncludesAllParts() {
    assertEquals("[INFO] [Game] started", entry(FlixelLogLevel.INFO, "Game", "started").toString());
  }

  @Test
  void toStringWithEmptyTagOmitsTagBrackets() {
    assertEquals("[DEBUG] debug message", entry(FlixelLogLevel.DEBUG, "", "debug message").toString());
  }

  @Test
  void siteIsUnknownWithoutAResolver() {
    FlixelLogSite site = entry(FlixelLogLevel.INFO, "", "x").site();
    assertEquals("Unknown", site.getFileName());
    assertEquals(0, site.getLine());
    assertEquals("", site.getClassName());
    assertEquals("unknown", site.getMethodName());
  }

  @Test
  void siteIsUnknownWhenTheResolverReturnsFalseOrThrows() {
    FlixelLogEntry e = entry(FlixelLogLevel.INFO, "", "x");
    e.reset(1L, FlixelLogLevel.INFO, FlixelLogMode.SIMPLE, "", null, out -> false);
    assertEquals("Unknown", e.site().getFileName());

    e.reset(1L, FlixelLogLevel.INFO, FlixelLogMode.SIMPLE, "", null, out -> {
      throw new IllegalStateException("nope");
    });
    assertEquals("Unknown", e.site().getFileName());
  }

  @Test
  void siteResolvesOncePerEntryAndAgainAfterReset() {
    int[] calls = new int[1];
    FlixelLogSiteResolver resolver = out -> {
      calls[0]++;
      out.set("A.java", 5, "p.A", "run");
      return true;
    };
    FlixelLogEntry e = new FlixelLogEntry(new FlixelString("m"));
    e.reset(1L, FlixelLogLevel.INFO, FlixelLogMode.SIMPLE, "", null, resolver);
    assertSame(e.site(), e.site());
    assertEquals(1, calls[0]);
    assertEquals(5, e.site().getLine());

    e.reset(2L, FlixelLogLevel.INFO, FlixelLogMode.SIMPLE, "", null, resolver);
    e.site();
    assertEquals(2, calls[0]);
  }

  @Test
  void siteSetterNormalizesNullsAndNegativeLines() {
    FlixelLogSite site = new FlixelLogSite();
    site.set(null, -4, null, null);
    assertEquals("Unknown", site.getFileName());
    assertEquals(0, site.getLine());
    assertEquals("", site.getClassName());
    assertEquals("unknown", site.getMethodName());
  }

  @Test
  void siteMarkerMarkResolveAndClear() {
    FlixelLogSiteMarker.clear();
    FlixelLogSite out = new FlixelLogSite();
    assertFalse(FlixelLogSiteMarker.INSTANCE.resolve(out));

    FlixelLogSiteMarker.mark("Hello.java", 9, "a.b.Hello", "greet");
    assertTrue(FlixelLogSiteMarker.INSTANCE.resolve(out));
    assertEquals("Hello.java", out.getFileName());
    assertEquals(9, out.getLine());
    assertEquals("a.b.Hello", out.getClassName());
    assertEquals("greet", out.getMethodName());
    assertFalse(FlixelLogSiteMarker.INSTANCE.resolve(out));

    FlixelLogSiteMarker.mark("Hello.java", 9, "a.b.Hello", "greet");
    FlixelLogSiteMarker.clear();
    assertFalse(FlixelLogSiteMarker.INSTANCE.resolve(out));
  }

  @Test
  void levelSeverityIsDefinedExplicitly() {
    assertEquals(0, FlixelLogLevel.DEBUG.getSeverity());
    assertEquals(1, FlixelLogLevel.INFO.getSeverity());
    assertEquals(2, FlixelLogLevel.WARN.getSeverity());
    assertEquals(3, FlixelLogLevel.ERROR.getSeverity());
  }

  @Test
  void simpleFormatUsesPackagePathFileAndLine() {
    FlixelLogEntry e = entry(FlixelLogLevel.INFO, "", "hello");
    e.reset(1L, FlixelLogLevel.INFO, FlixelLogMode.SIMPLE, "", null, out -> {
      out.set("MyState.java", 42, "org.game.states.MyState", "create");
      return true;
    });
    FlixelString out = new FlixelString();
    FlixelLogFormat.appendSimple(out, e);
    assertEquals("org/game/states/MyState.java:42: hello", out.toString());
  }

  @Test
  void simpleFormatWithoutAPackageOmitsThePath() {
    FlixelLogEntry e = entry(FlixelLogLevel.INFO, "", "hello");
    e.reset(1L, FlixelLogLevel.INFO, FlixelLogMode.SIMPLE, "", null, out -> {
      out.set("Main.java", 3, "Main", "main");
      return true;
    });
    FlixelString out = new FlixelString();
    FlixelLogFormat.appendSimple(out, e);
    assertEquals("Main.java:3: hello", out.toString());
  }

  @Test
  void detailedFormatMatchesTheDocumentedLayout() {
    long time = 1_700_000_000_123L;
    FlixelLogEntry e = entry(FlixelLogLevel.WARN, "AI", "path blocked");
    e.reset(time, FlixelLogLevel.WARN, FlixelLogMode.DETAILED, "AI", null, out -> {
      out.set("Pathing.java", 88, "org.game.Pathing", "solve");
      return true;
    });
    FlixelString out = new FlixelString();
    FlixelLogFormat.appendDetailed(out, e);
    String stamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()).format(STAMP);
    assertEquals(stamp + " [WARN] [AI] [Pathing.java:88] [solve()] path blocked", out.toString());
  }

  @Test
  void timestampMatchesLocalDateTimeForManyInstants() {
    long[] samples = {
        0L, 1L, 999L, 1_000L, 59_999L, 3_599_999L, 3_600_000L, 86_399_999L, 86_400_000L,
        951_782_399_999L, 951_782_400_000L, 1_700_000_000_000L, 1_709_251_199_999L, 1_709_251_200_000L,
        4_102_444_799_999L, 4_102_444_800_000L, -1L, -86_400_001L
    };
    FlixelString out = new FlixelString();
    for (long ms : samples) {
      out.clear();
      FlixelLogFormat.appendTimestamp(out, ms);
      assertEquals(format(ms), out.toString(), "for " + ms);
    }
    long ms = 1_700_000_000_000L;
    for (int i = 0; i < 5000; i++) {
      ms += 617_311L;
      out.clear();
      FlixelLogFormat.appendTimestamp(out, ms);
      assertEquals(format(ms), out.toString(), "for " + ms);
    }
    out.clear();
    FlixelLogFormat.appendTimestamp(out, 1_700_000_000_000L);
    assertEquals(format(1_700_000_000_000L), out.toString());
  }

  @Test
  void timestampFollowsDaylightSavingChanges() {
    TimeZone original = TimeZone.getDefault();
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
      long beforeSpringForward = Instant.parse("2024-03-10T06:59:59.500Z").toEpochMilli();
      long afterSpringForward = Instant.parse("2024-03-10T07:00:00.250Z").toEpochMilli();
      long beforeFallBack = Instant.parse("2024-11-03T05:59:59.900Z").toEpochMilli();
      long afterFallBack = Instant.parse("2024-11-03T06:00:00.100Z").toEpochMilli();
      long inRepeatedHour = Instant.parse("2024-11-03T06:30:00.000Z").toEpochMilli();
      FlixelString out = new FlixelString();
      for (long ms : new long[] {
          beforeSpringForward, afterSpringForward, beforeFallBack, afterFallBack, inRepeatedHour, beforeFallBack
      }) {
        out.clear();
        FlixelLogFormat.appendTimestamp(out, ms);
        assertEquals(format(ms), out.toString(), "for " + Instant.ofEpochMilli(ms));
      }
    } finally {
      TimeZone.setDefault(original);
      FlixelString out = new FlixelString();
      FlixelLogFormat.appendTimestamp(out, 1_700_000_000_000L);
      assertEquals(format(1_700_000_000_000L), out.toString());
    }
  }

  @Test
  void throwableFormatStartsOnANewLineAndHasNoTrailingBreak() {
    FlixelString out = new FlixelString("msg");
    FlixelLogFormat.appendThrowable(out, new IllegalStateException("bad state"));
    String text = out.toString();
    assertTrue(text.startsWith("msg\nException: java.lang.IllegalStateException: bad state"), text);
    assertTrue(text.contains("Stack Trace:"), text);
    assertFalse(text.endsWith("\n"), text);
  }

  @Test
  void plainConsoleSinkPrintsOneLinePerEntryAndTheTrace() {
    FlixelDefaultLogger logger = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    logger.setSiteResolver(out -> {
      out.set("Game.java", 7, "org.game.Game", "tick");
      return true;
    });
    PrintStream originalOut = System.out;
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    System.setOut(new PrintStream(bytes, true));
    try {
      logger.info("hello {}", "world");
      logger.error("broke", new RuntimeException("boom"));
      logger.setMode(FlixelLogMode.DETAILED);
      logger.setTag("T");
      logger.warn("detailed");
    } finally {
      System.setOut(originalOut);
    }
    String[] lines = bytes.toString().replace("\r", "").split("\n");
    assertEquals("org/game/Game.java:7: hello world", lines[0]);
    assertEquals("org/game/Game.java:7: broke", lines[1]);
    assertTrue(lines[2].startsWith("Exception: java.lang.RuntimeException: boom"), lines[2]);
    String last = lines[lines.length - 1];
    assertTrue(last.endsWith(" [WARN] [T] [Game.java:7] [tick()] detailed"), last);
    assertNotNull(logger.getConsoleSink());
    assertNull(logger.getFileSink());
  }

  private static String format(long epochMillis) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()).format(STAMP);
  }
}
