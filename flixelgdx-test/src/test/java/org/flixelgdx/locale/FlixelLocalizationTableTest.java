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
package org.flixelgdx.locale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class FlixelLocalizationTableTest {

  @Test
  void flattensNestedObjectsAndArrays() {
    FlixelLocalizationTable table = new FlixelLocalizationTable("en").putJson("""
        { "a": { "b": { "c": "deep" } }, "lines": ["one", "two"], "top": "level" }
        """);
    assertEquals("deep", table.get("a.b.c"));
    assertEquals("one", table.get("lines.0"));
    assertEquals("two", table.get("lines.1"));
    assertEquals("level", table.get("top"));
    assertEquals(4, table.getSize());
  }

  @Test
  void storesScalarsAsTextAndSkipsNull() {
    FlixelLocalizationTable table = new FlixelLocalizationTable("en").putJson("""
        { "whole": 3, "frac": 2.5, "flag": true, "gone": null }
        """);
    assertEquals("3", table.get("whole"));
    assertEquals("2.5", table.get("frac"));
    assertEquals("true", table.get("flag"));
    assertFalse(table.has("gone"));
  }

  @Test
  void normalizesTag() {
    assertEquals("pt-BR", new FlixelLocalizationTable(" pt_BR ").getTag());
  }

  @Test
  void removeAndClear() {
    FlixelLocalizationTable table = new FlixelLocalizationTable("en").put("a", "1").put("b", "2");
    assertEquals("1", table.remove("a"));
    assertNull(table.get("a"));
    table.clear();
    assertEquals(0, table.getSize());
  }
}
