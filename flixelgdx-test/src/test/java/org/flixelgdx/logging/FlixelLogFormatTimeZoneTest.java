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

import org.flixelgdx.Flixel;
import org.flixelgdx.backend.FlixelRuntimeDevice;
import org.flixelgdx.util.FlixelString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks that log timestamps use the offset reported by the runtime device, not the Java default
 * time zone. The formatter caches the current hour, so each test clears that cache first.
 */
class FlixelLogFormatTimeZoneTest {

  private static final int MINUTE_MS = 60_000;
  private static final int HOUR_MS = 3_600_000;

  private FlixelRuntimeDevice original;

  @BeforeEach
  void saveDevice() {
    original = Flixel.runtime;
    FlixelLogFormat.resetCache();
  }

  @AfterEach
  void restoreDevice() {
    Flixel.runtime = original;
    FlixelLogFormat.resetCache();
  }

  private static String stamp(String utcInstant) {
    FlixelString out = new FlixelString();
    FlixelLogFormat.appendTimestamp(out, Instant.parse(utcInstant).toEpochMilli());
    return out.toString();
  }

  private static String systemStamp(String utcInstant) {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        .format(LocalDateTime.ofInstant(Instant.parse(utcInstant), ZoneId.systemDefault()));
  }

  private static void useOffset(int offsetMs) {
    useOffsetKeepingCache(offsetMs);
    FlixelLogFormat.resetCache();
  }

  private static void useOffsetKeepingCache(int offsetMs) {
    Flixel.runtime = new FlixelRuntimeDevice() {
      @Override
      public int getUtcOffsetMillis(long epochMillis) {
        return offsetMs;
      }
    };
  }

  @Test
  void negativeOffsetShiftsTheClockBackAndAcrossTheDayBoundary() {
    useOffset(-5 * HOUR_MS);
    assertEquals("2024-05-31 22:07:09.123", stamp("2024-06-01T03:07:09.123Z"));
    assertEquals("2024-05-31 23:59:59.999", stamp("2024-06-01T04:59:59.999Z"));
    assertEquals("2024-06-01 00:00:00.000", stamp("2024-06-01T05:00:00.000Z"));
  }

  @Test
  void positiveOffsetShiftsTheClockForwardAndAcrossTheYearBoundary() {
    useOffset(9 * HOUR_MS);
    assertEquals("2025-01-01 08:30:00.500", stamp("2024-12-31T23:30:00.500Z"));
  }

  @Test
  void offsetsThatAreNotWholeHoursKeepTheMinutes() {
    useOffset(5 * HOUR_MS + 30 * MINUTE_MS);
    assertEquals("2024-06-01 05:30:00.000", stamp("2024-06-01T00:00:00.000Z"));
    assertEquals("2024-06-01 06:15:42.007", stamp("2024-06-01T00:45:42.007Z"));
  }

  @Test
  void zeroOffsetPrintsUtc() {
    useOffset(0);
    assertEquals("2024-06-01 03:07:09.123", stamp("2024-06-01T03:07:09.123Z"));
  }

  @Test
  void offsetIsLookedUpAgainWhenTheHourChanges() {
    long jump = Instant.parse("2024-03-10T07:00:00Z").toEpochMilli();
    // Mimics a daylight saving jump: UTC-5 before 07:00 UTC, UTC-4 from then on.
    Flixel.runtime = new FlixelRuntimeDevice() {
      @Override
      public int getUtcOffsetMillis(long epochMillis) {
        return epochMillis < jump ? -5 * HOUR_MS : -4 * HOUR_MS;
      }
    };
    FlixelLogFormat.resetCache();
    assertEquals("2024-03-10 01:59:59.900", stamp("2024-03-10T06:59:59.900Z"));
    assertEquals("2024-03-10 03:00:00.100", stamp("2024-03-10T07:00:00.100Z"));
  }

  @Test
  void swappingTheRuntimeDeviceRefreshesTheCachedHour() {
    useOffset(0);
    assertEquals("2024-06-01 03:07:09.123", stamp("2024-06-01T03:07:09.123Z"));
    useOffsetKeepingCache(-5 * HOUR_MS);
    assertEquals("2024-05-31 22:07:10.123", stamp("2024-06-01T03:07:10.123Z"));
  }

  @Test
  void defaultDeviceMatchesTheJavaSystemTimeZone() {
    Flixel.runtime = new FlixelRuntimeDevice() {
    };
    FlixelLogFormat.resetCache();
    assertEquals(systemStamp("2024-07-04T12:34:56.789Z"), stamp("2024-07-04T12:34:56.789Z"));
  }

  @Test
  void aNullRuntimeFallsBackToTheDefaultOffset() {
    Flixel.runtime = null;
    FlixelLogFormat.resetCache();
    assertEquals(systemStamp("2024-07-04T12:34:56.789Z"), stamp("2024-07-04T12:34:56.789Z"));
  }
}
