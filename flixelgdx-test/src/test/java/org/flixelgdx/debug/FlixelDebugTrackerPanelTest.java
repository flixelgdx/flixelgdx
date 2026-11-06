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

import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugTrackerPanelTest {

  @Test
  void rowsAreSortedByKeyAndEmptyEntriesAreLeftOut() {
    FlixelDebugTrackerPanel trackers = new FlixelDebugManager().trackers;
    trackers.add(new MapEntry("Enemies", "zeta", "1", "alpha", "2", "mid", "3"));
    trackers.add(new MapEntry("Hidden"));
    trackers.add(new MapEntry("Items", "b", "x", "a", "y"));
    assertEquals(3, trackers.getCount());

    FlixelDebugMessageReader r = DebugTestSupport.payload(trackers);
    assertEquals(2, r.readCount());

    assertEquals("Enemies", DebugTestSupport.str(r));
    assertEquals(3, r.readCount());
    String[][] expected = { { "alpha", "2" }, { "mid", "3" }, { "zeta", "1" } };
    for (String[] row : expected) {
      assertEquals(row[0], DebugTestSupport.str(r));
      assertEquals(FlixelDebugProtocol.VALUE_STRING, r.readU8());
      assertEquals(row[1], DebugTestSupport.str(r));
    }

    assertEquals("Items", DebugTestSupport.str(r));
    assertEquals(2, r.readCount());
    assertEquals("a", DebugTestSupport.str(r));
    r.readU8();
    assertEquals("y", DebugTestSupport.str(r));
    assertEquals("b", DebugTestSupport.str(r));
    r.readU8();
    assertEquals("x", DebugTestSupport.str(r));
    assertEquals(0, r.getRemaining());
  }

  @Test
  void addTwiceIsIgnoredAndRemoveWorks() {
    FlixelDebugTrackerPanel trackers = new FlixelDebugManager().trackers;
    MapEntry entry = new MapEntry("A", "k", "v");
    trackers.add(entry);
    trackers.add(entry);
    assertEquals(1, trackers.getCount());
    long revision = trackers.getRevision();
    trackers.remove(entry);
    trackers.remove(entry);
    assertEquals(0, trackers.getCount());
    assertTrue(trackers.getRevision() > revision);
    assertEquals(0, DebugTestSupport.payload(trackers).readCount());
  }

  private static final class MapEntry extends FlixelDebugTrackerEntry {

    private final FlixelMap<String, String> values = new FlixelMap<>();

    MapEntry(String name, String... pairs) {
      super(name);
      for (int i = 0; i < pairs.length; i += 2) {
        values.put(pairs[i], pairs[i + 1]);
      }
    }

    @Override
    public FlixelMap<String, String> getTrackedValues() {
      return values;
    }
  }
}
