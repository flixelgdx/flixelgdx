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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlixelLogEntryTest {

  @Test
  void recordAccessorsReturnConstructorValues() {
    FlixelLogEntry entry = new FlixelLogEntry(FlixelLogLevel.WARN, "MyTag", "something broke");
    assertEquals(FlixelLogLevel.WARN, entry.level());
    assertEquals("MyTag", entry.tag());
    assertEquals("something broke", entry.message());
  }

  @Test
  void toStringWithTagIncludesAllParts() {
    FlixelLogEntry entry = new FlixelLogEntry(FlixelLogLevel.INFO, "Game", "started");
    assertEquals("[INFO] [Game] started", entry.toString());
  }

  @Test
  void toStringWithEmptyTagOmitsTagBrackets() {
    FlixelLogEntry entry = new FlixelLogEntry(FlixelLogLevel.DEBUG, "", "debug message");
    assertEquals("[DEBUG] debug message", entry.toString());
  }

  @Test
  void toStringForErrorLevel() {
    FlixelLogEntry entry = new FlixelLogEntry(FlixelLogLevel.ERROR, "Crash", "null pointer");
    assertEquals("[ERROR] [Crash] null pointer", entry.toString());
  }

  @Test
  void recordEqualityHoldsForIdenticalValues() {
    FlixelLogEntry a = new FlixelLogEntry(FlixelLogLevel.INFO, "tag", "msg");
    FlixelLogEntry b = new FlixelLogEntry(FlixelLogLevel.INFO, "tag", "msg");
    assertEquals(a, b);
  }
}
