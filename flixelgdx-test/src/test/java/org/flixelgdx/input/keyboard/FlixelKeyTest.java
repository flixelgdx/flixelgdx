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
package org.flixelgdx.input.keyboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlixelKeyTest {

  @Test
  void namedCodesRoundTrip() {
    for (int code = 0; code <= FlixelKey.MAX_KEYCODE; code++) {
      String name = FlixelKey.toString(code);
      if (!name.equals("UNKNOWN")) {
        assertEquals(code, FlixelKey.fromString(name),
            "toString/fromString must round-trip for code " + code + " (" + name + ")");
      }
    }
  }

  @Test
  void reservedCodesHaveNames() {
    assertEquals("NONE", FlixelKey.toString(FlixelKey.NONE));
    assertEquals("ANY", FlixelKey.toString(FlixelKey.ANY));
    assertEquals(FlixelKey.NONE, FlixelKey.fromString("NONE"));
    assertEquals(FlixelKey.ANY, FlixelKey.fromString("ANY"));
  }

  @Test
  void aliasesResolveToSharedCode() {
    assertEquals(FlixelKey.DEL, FlixelKey.fromString("BACKSPACE"));
  }

  @Test
  void unknownNamesAndCodesAreHandled() {
    assertEquals(FlixelKey.NONE, FlixelKey.fromString("NOT_A_KEY"));
    assertEquals(FlixelKey.NONE, FlixelKey.fromString(null));
    assertEquals("UNKNOWN", FlixelKey.toString(9999));
  }
}
