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

import org.flixelgdx.debug.panels.FlixelDebugWatchPanel;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.util.FlixelString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugWatchPanelTest {

  private FlixelDebugWatchPanel watches;

  @BeforeEach
  void setUp() {
    watches = new FlixelDebugManager().watches;
  }

  @Test
  void namedAddMethodsWriteTypedValuesInOrder() {
    watches.addInt("int", () -> 42);
    watches.addLong("long", () -> 1L << 40);
    watches.addFloat("float", () -> 1.5f);
    watches.addDouble("double", () -> 2.25);
    watches.addBoolean("bool", () -> true);
    watches.addByte("byte", () -> (byte) -7);
    watches.addShort("short", () -> (short) 300);
    watches.addChar("char", () -> 'Z');

    FlixelDebugMessageReader r = DebugTestSupport.payload(watches);
    assertEquals(8, r.readCount());

    assertEquals("int", DebugTestSupport.str(r));
    assertEquals(FlixelDebugProtocol.VALUE_INT, r.readU8());
    assertEquals(42, r.readI32());

    assertEquals("long", DebugTestSupport.str(r));
    assertEquals(FlixelDebugProtocol.VALUE_LONG, r.readU8());
    assertEquals(1L << 40, r.readI64());

    assertEquals("float", DebugTestSupport.str(r));
    assertEquals(FlixelDebugProtocol.VALUE_FLOAT, r.readU8());
    assertEquals(1.5f, r.readF32());

    assertEquals("double", DebugTestSupport.str(r));
    assertEquals(FlixelDebugProtocol.VALUE_DOUBLE, r.readU8());
    assertEquals(2.25, r.readF64());

    assertEquals("bool", DebugTestSupport.str(r));
    assertEquals(FlixelDebugProtocol.VALUE_BOOL, r.readU8());
    assertTrue(r.readBool());

    assertEquals("byte", DebugTestSupport.str(r));
    assertEquals(FlixelDebugProtocol.VALUE_INT, r.readU8());
    assertEquals(-7, r.readI32());

    assertEquals("short", DebugTestSupport.str(r));
    assertEquals(FlixelDebugProtocol.VALUE_INT, r.readU8());
    assertEquals(300, r.readI32());

    assertEquals("char", DebugTestSupport.str(r));
    assertEquals(FlixelDebugProtocol.VALUE_STRING, r.readU8());
    assertEquals("Z", DebugTestSupport.str(r));

    assertEquals(0, r.getRemaining());
  }

  @Test
  void objectWatchesAreTypedByTheirValue() {
    watches.add("integer", () -> Integer.valueOf(5));
    watches.add("text", () -> "hello");
    watches.add("object", () -> new Object() {
      @Override
      public String toString() {
        return "custom";
      }
    });
    watches.add("null", () -> null);
    watches.add("flag", () -> Boolean.FALSE);

    FlixelDebugMessageReader r = DebugTestSupport.payload(watches);
    assertEquals(5, r.readCount());
    assertEquals("integer", DebugTestSupport.str(r));
    assertEquals(FlixelDebugProtocol.VALUE_INT, r.readU8());
    assertEquals(5, r.readI32());
    assertEquals("text", DebugTestSupport.str(r));
    assertEquals(FlixelDebugProtocol.VALUE_STRING, r.readU8());
    assertEquals("hello", DebugTestSupport.str(r));
    assertEquals("object", DebugTestSupport.str(r));
    assertEquals(FlixelDebugProtocol.VALUE_STRING, r.readU8());
    assertEquals("custom", DebugTestSupport.str(r));
    assertEquals("null", DebugTestSupport.str(r));
    assertEquals(FlixelDebugProtocol.VALUE_STRING, r.readU8());
    assertEquals("null", DebugTestSupport.str(r));
    assertEquals("flag", DebugTestSupport.str(r));
    assertEquals(FlixelDebugProtocol.VALUE_BOOL, r.readU8());
    assertFalse(r.readBool());
  }

  @Test
  void aGetterThatThrowsWritesAnErrorValueAndTheRestStillWork() {
    watches.addInt("before", () -> 1);
    watches.addInt("bad", () -> {
      throw new IllegalStateException("no player yet");
    });
    watches.add("worse", () -> {
      throw new NullPointerException();
    });
    watches.addInt("after", () -> 2);

    FlixelDebugMessageReader r = DebugTestSupport.payload(watches);
    assertEquals(4, r.readCount());
    assertEquals("before", DebugTestSupport.str(r));
    assertEquals(FlixelDebugProtocol.VALUE_INT, r.readU8());
    assertEquals(1, r.readI32());

    assertEquals("bad", DebugTestSupport.str(r));
    assertEquals(FlixelDebugProtocol.VALUE_ERROR, r.readU8());
    assertEquals("no player yet", DebugTestSupport.str(r));

    assertEquals("worse", DebugTestSupport.str(r));
    assertEquals(FlixelDebugProtocol.VALUE_ERROR, r.readU8());
    assertEquals(NullPointerException.class.getName(), DebugTestSupport.str(r));

    assertEquals("after", DebugTestSupport.str(r));
    assertEquals(FlixelDebugProtocol.VALUE_INT, r.readU8());
    assertEquals(2, r.readI32());
    assertEquals(0, r.getRemaining());
  }

  @Test
  void addingTheSameNameReplacesTheWatchInPlace() {
    watches.addInt("a", () -> 1);
    watches.addInt("b", () -> 2);
    watches.addInt("a", () -> 3);
    assertEquals(2, watches.getCount());

    FlixelDebugMessageReader r = DebugTestSupport.payload(watches);
    assertEquals(2, r.readCount());
    assertEquals("a", DebugTestSupport.str(r));
    r.readU8();
    assertEquals(3, r.readI32());
    assertEquals("b", DebugTestSupport.str(r));
  }

  @Test
  void removeAndClear() {
    watches.addInt("a", () -> 1);
    watches.addInt("b", () -> 2);
    assertTrue(watches.remove("a"));
    assertFalse(watches.remove("a"));
    assertFalse(watches.has("a"));
    assertTrue(watches.has("b"));
    watches.clear();
    assertEquals(0, watches.getCount());
    assertEquals(0, DebugTestSupport.payload(watches).readCount());
  }

  @Test
  void mouseAndCameraWatchesCanBeAddedAndRemoved() {
    watches.addMouse();
    watches.addCamera();
    assertTrue(watches.isMouseWatched());
    assertTrue(watches.isCameraWatched());
    assertEquals(2, watches.getCount());
    watches.removeMouse();
    watches.removeCamera();
    assertEquals(0, watches.getCount());
  }

  @Test
  void mouseWatchWritesAPositionString() {
    watches.addMouse();
    FlixelDebugMessageReader r = DebugTestSupport.payload(watches);
    assertEquals(1, r.readCount());
    assertEquals("Mouse", DebugTestSupport.str(r));
    assertEquals(FlixelDebugProtocol.VALUE_STRING, r.readU8());
    assertTrue(DebugTestSupport.str(r).matches("-?\\d+, -?\\d+"));
  }

  @Test
  void changesBumpTheRevisionAndUpdateOffersFreshValuesOnATimer() {
    long start = watches.getRevision();
    watches.addInt("a", () -> 1);
    long afterAdd = watches.getRevision();
    assertTrue(afterAdd > start);

    watches.update(0.01f);
    assertEquals(afterAdd, watches.getRevision());
    watches.update(0.2f);
    assertTrue(watches.getRevision() > afterAdd);

    watches.clear();
    long empty = watches.getRevision();
    watches.update(0.5f);
    assertEquals(empty, watches.getRevision(), "an empty list has nothing to refresh");
  }

  @Test
  void numbersFormatAsIntegersWhenWholeAndTwoDecimalsOtherwise() {
    FlixelString s = new FlixelString();
    FlixelDebugWatchPanel.appendNumber(s, 3.0);
    s.concat('|');
    FlixelDebugWatchPanel.appendNumber(s, -4.0);
    s.concat('|');
    FlixelDebugWatchPanel.appendNumber(s, 2.5);
    s.concat('|');
    FlixelDebugWatchPanel.appendNumber(s, 0.333);
    s.concat('|');
    FlixelDebugWatchPanel.appendNumber(s, 100f);
    assertEquals("3|-4|2.50|0.33|100", s.toString());
  }
}
