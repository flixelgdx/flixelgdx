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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelJvmLogFileSinkTest {

  private static final String DROP_SUFFIX = " log lines were dropped because the queue was full]";

  @TempDir
  Path dir;

  private FlixelDefaultLogger logger;

  @BeforeEach
  void setUp() {
    logger = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    logger.setConsoleSink(null);
    logger.setSiteResolver(new FlixelJvmLogSiteResolver());
    logger.setTag("Test");
  }

  @Test
  void writesDetailedLinesWithTheStackTrace() throws IOException {
    FlixelJvmLogFileSink sink = new FlixelJvmLogFileSink();
    logger.setFileSink(sink);
    sink.open(dir.toString(), 5);
    assertTrue(sink.isOpen());

    logger.info("hello {}", 1);
    logger.error("failed", new IllegalStateException("boom"));
    sink.close();
    assertFalse(sink.isOpen());

    List<String> lines = Files.readAllLines(onlyLogFile(), StandardCharsets.UTF_8);
    assertTrue(lines.size() > 2, lines.toString());
    String first = lines.get(0);
    assertTrue(first.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[INFO\\] \\[Test\\] "
        + "\\[FlixelJvmLogFileSinkTest\\.java:\\d+\\] \\[writesDetailedLinesWithTheStackTrace\\(\\)\\] hello 1"),
        first);
    String second = lines.get(1);
    assertTrue(second.contains("[ERROR] [Test]"), second);
    assertTrue(second.endsWith("] failed"), second);
    String rest = String.join("\n", lines.subList(2, lines.size()));
    assertTrue(rest.contains("IllegalStateException"), rest);
    assertTrue(rest.contains("boom"), rest);
    assertTrue(rest.contains("writesDetailedLinesWithTheStackTrace"), rest);
  }

  @Test
  void closeFlushesEveryQueuedLine() throws IOException {
    FlixelJvmLogFileSink sink = new FlixelJvmLogFileSink();
    logger.setFileSink(sink);
    sink.open(dir.toString(), 5);
    for (int i = 0; i < 500; i++) {
      logger.info("line {}", i);
    }
    sink.close();

    List<String> lines = Files.readAllLines(onlyLogFile(), StandardCharsets.UTF_8);
    assertEquals(500, lines.size());
    assertTrue(lines.get(0).endsWith("line 0"));
    assertTrue(lines.get(499).endsWith("line 499"));
    assertEquals(0L, sink.getDroppedCount());
  }

  @Test
  void writesAfterCloseAreIgnored() throws IOException {
    FlixelJvmLogFileSink sink = new FlixelJvmLogFileSink();
    logger.setFileSink(sink);
    sink.open(dir.toString(), 5);
    logger.info("kept");
    sink.close();
    logger.info("ignored");

    List<String> lines = Files.readAllLines(onlyLogFile(), StandardCharsets.UTF_8);
    assertEquals(1, lines.size());
    assertTrue(lines.get(0).endsWith("kept"));
  }

  @Test
  void openingTwiceDoesNotCreateASecondFile() {
    FlixelJvmLogFileSink sink = new FlixelJvmLogFileSink();
    sink.open(dir.toString(), 5);
    String path = sink.getLogFilePath();
    sink.open(dir.toString(), 5);
    sink.close();
    assertNotNull(path);
    assertEquals(path, sink.getLogFilePath());
    assertEquals(1, logFiles().length);
  }

  @Test
  void nameFollowsTheTimestampPattern() {
    FlixelJvmLogFileSink sink = new FlixelJvmLogFileSink();
    sink.open(dir.toString(), 5);
    sink.close();
    String name = logFiles()[0].getName();
    assertTrue(name.matches("flixel-\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.log"), name);
  }

  @Test
  void pruningOnlyDeletesTheOldestFlixelLogFiles() throws IOException {
    String[] oldLogs = {
        "flixel-2020-01-01_00-00-00.log",
        "flixel-2020-01-02_00-00-00.log",
        "flixel-2020-01-03_00-00-00.log",
        "flixel-2020-01-04_00-00-00.log"
    };
    String[] others = { "notes.txt", "save.dat", "flixel-notes.txt", "other.log", "a-flixel-x.log" };
    for (String name : oldLogs) {
      Files.writeString(dir.resolve(name), "old");
    }
    for (String name : others) {
      Files.writeString(dir.resolve(name), "keep me");
    }

    FlixelJvmLogFileSink sink = new FlixelJvmLogFileSink();
    sink.open(dir.toString(), 3);
    sink.close();

    for (String name : others) {
      assertTrue(Files.exists(dir.resolve(name)), name + " must not be deleted");
    }
    assertFalse(Files.exists(dir.resolve(oldLogs[0])));
    assertFalse(Files.exists(dir.resolve(oldLogs[1])));
    assertTrue(Files.exists(dir.resolve(oldLogs[2])));
    assertTrue(Files.exists(dir.resolve(oldLogs[3])));
    assertEquals(3, logFiles().length);
  }

  @Test
  void tinyCapacityDropsLinesAndWritesAMarker() throws IOException {
    FlixelJvmLogFileSink sink = new FlixelJvmLogFileSink(1);
    assertEquals(1, sink.getCapacity());
    logger.setFileSink(sink);
    sink.open(dir.toString(), 5);
    int total = 20000;
    for (int i = 0; i < total; i++) {
      logger.info("line {}", i);
    }
    sink.close();

    int written = 0;
    long markerTotal = 0;
    for (String line : Files.readAllLines(onlyLogFile(), StandardCharsets.UTF_8)) {
      if (line.startsWith("[") && line.endsWith(DROP_SUFFIX)) {
        markerTotal += Long.parseLong(line.substring(1, line.length() - DROP_SUFFIX.length()));
      } else {
        written++;
      }
    }
    assertTrue(markerTotal > 0, "A one-slot queue should have dropped lines");
    assertEquals(markerTotal, sink.getDroppedCount());
    assertEquals(total, written + markerTotal);
  }

  private File[] logFiles() {
    File[] files = dir.toFile().listFiles((d, name) -> name.startsWith("flixel-") && name.endsWith(".log"));
    assertNotNull(files);
    Arrays.sort(files);
    return files;
  }

  private Path onlyLogFile() {
    File[] files = logFiles();
    assertEquals(1, files.length);
    return files[0].toPath();
  }
}
