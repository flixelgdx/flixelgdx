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
package org.flixelgdx.save;

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.json.FlixelJson;
import org.flixelgdx.json.FlixelJsonValue;
import org.flixelgdx.testing.FlixelMemorySaveHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelSaveTest {

  @Test
  void bindFlushRoundTrip() {
    FlixelSaveHandler handler = new FlixelMemorySaveHandler();
    String name = "flixelgdx_junit_save_" + System.nanoTime();
    FlixelSave a = new FlixelSave(handler);
    assertTrue(a.bind(name, null));
    a.data.put("score", 42);
    assertTrue(a.flush());

    FlixelSave b = new FlixelSave(handler);
    assertTrue(b.bind(name, null));
    assertEquals(FlixelSaveStatus.OK, b.getStatus());
    Object v = b.data.get("score");
    Assertions.assertNotNull(v);
    assertEquals(42.0, ((Number) v).doubleValue(), 1e-6);
  }

  @Test
  void nestedJsonStringRoundTrips() {
    FlixelSaveHandler handler = new FlixelMemorySaveHandler();
    String json = "{\"startFullscreen\":true,\"masterVolume\":0.5}";
    FlixelSave a = new FlixelSave(handler);
    assertTrue(a.bind("MIC", "settings"));
    a.data.put("settings", json);
    assertTrue(a.flush());
    assertTrue(handler.exists(a.getKey()));

    FlixelSave b = new FlixelSave(handler);
    assertTrue(b.bind("MIC", "settings"));
    assertEquals(json, b.getString("settings", null));
  }

  @Test
  void eraseRemovesStoredSave() {
    FlixelSaveHandler handler = new FlixelMemorySaveHandler();
    FlixelSave s = new FlixelSave(handler);
    assertTrue(s.bind("flixelgdx_junit_erase", null));
    s.data.put("k", 1);
    assertTrue(s.flush());
    assertTrue(handler.exists(s.getKey()));
    assertTrue(s.erase());
    assertFalse(handler.exists(s.getKey()));
    assertEquals(FlixelSaveStatus.EMPTY, s.getStatus());
  }

  @Test
  void noopHandlerStoresNothing() {
    FlixelSave s = new FlixelSave(FlixelNoopSaveHandler.INSTANCE);
    assertTrue(s.bind("flixelgdx_junit_noop", null));
    s.data.put("k", 1);
    assertFalse(s.flush());
    assertEquals(FlixelSaveStatus.ERROR, s.getStatus());
  }

  @Test
  void mergeDataRespectsOverwrite() {
    FlixelSave s = new FlixelSave(new FlixelMemorySaveHandler());
    assertTrue(s.bind("flixelgdx_junit_merge_" + System.nanoTime(), null));
    s.data.put("k", "a");
    FlixelMap<String, Object> in = new FlixelMap<>();
    in.put("k", "b");
    in.put("other", 1);
    s.mergeData(in, false, false);
    assertEquals("a", s.data.get("k"));
    s.mergeData(in, true, false);
    assertEquals("b", s.data.get("k"));
  }

  @Test
  void toJsonCopiesData() {
    FlixelSave s = new FlixelSave(FlixelNoopSaveHandler.INSTANCE);
    FlixelMap<String, Object> options = new FlixelMap<>();
    options.put("volume", 0.5);
    FlixelArray<Object> levels = new FlixelArray<>();
    levels.add("forest");
    s.data.put("score", 42);
    s.data.put("name", "Ada");
    s.data.put("options", options);
    s.data.put("levels", levels);

    FlixelJsonValue json = s.toJson();
    assertTrue(json.isObject());
    assertEquals(42, json.getInt("score", 0));
    assertEquals("Ada", json.getString("name", null));
    FlixelJsonValue opts = json.get("options");
    Assertions.assertNotNull(opts);
    assertEquals(0.5f, opts.getFloat("volume", 0f), 1e-6f);
    FlixelJsonValue list = json.get("levels");
    Assertions.assertNotNull(list);
    assertEquals(1, list.getSize());
  }

  @Test
  void fromJsonReplacesData() {
    FlixelSave s = new FlixelSave(FlixelNoopSaveHandler.INSTANCE);
    s.data.put("stale", true);
    s.fromJson(FlixelJson.parse("{\"score\":7,\"options\":{\"music\":false},\"levels\":[\"a\",\"b\"]}"));

    assertFalse(s.data.containsKey("stale"));
    assertEquals(7, s.getInt("score", 0));
    assertTrue(s.data.get("options") instanceof FlixelMap<?, ?>);
    Object levels = s.data.get("levels");
    assertTrue(levels instanceof FlixelArray<?>);
    assertEquals(2, ((FlixelArray<?>) levels).getSize());
  }

  @Test
  void fromJsonRejectsNonObjects() {
    FlixelSave s = new FlixelSave(FlixelNoopSaveHandler.INSTANCE);
    s.data.put("k", 1);
    assertThrows(IllegalArgumentException.class, () -> s.fromJson(FlixelJson.parse("[1,2]")));
    assertEquals(1, s.getInt("k", 0));
  }

  @Test
  void jsonRoundTripThroughFlush() {
    FlixelSaveHandler handler = new FlixelMemorySaveHandler();
    FlixelSave a = new FlixelSave(handler);
    assertTrue(a.bind("flixelgdx_junit_json", null));
    a.fromJson(FlixelJson.parse("{\"best\":99,\"done\":true}"));
    assertTrue(a.flush());

    FlixelSave b = new FlixelSave(handler);
    assertTrue(b.bind("flixelgdx_junit_json", null));
    FlixelJsonValue json = b.toJson();
    assertEquals(99, json.getInt("best", 0));
    assertTrue(json.getBool("done", false));
  }

  @Test
  void nonFiniteNumbersAreStoredAsNull() {
    FlixelSaveHandler handler = new FlixelMemorySaveHandler();
    FlixelSave a = new FlixelSave(handler);
    assertTrue(a.bind("flixelgdx_junit_nan", null));
    a.data.put("nan", Double.NaN);
    a.data.put("inf", Float.POSITIVE_INFINITY);
    a.data.put("score", 5);
    assertTrue(a.toJson().get("nan").isNull());
    assertTrue(a.flush());

    FlixelSave b = new FlixelSave(handler);
    assertTrue(b.bind("flixelgdx_junit_nan", null));
    assertEquals(FlixelSaveStatus.OK, b.getStatus());
    assertTrue(b.data.containsKey("nan"));
    assertNull(b.data.get("nan"));
    assertNull(b.data.get("inf"));
    assertEquals(5, b.getInt("score", 0));
  }

  @Test
  void newSavesUseDefaultHandler() {
    FlixelSaveHandler previous = FlixelSave.defaultHandler;
    try {
      FlixelSaveHandler handler = new FlixelMemorySaveHandler();
      FlixelSave.defaultHandler = handler;
      assertSame(handler, new FlixelSave().getHandler());
    } finally {
      FlixelSave.defaultHandler = previous;
    }
  }

  @Test
  void nullHandlersAreRejected() {
    FlixelSaveHandler handler = new FlixelMemorySaveHandler();
    FlixelSave s = new FlixelSave(handler);
    assertThrows(NullPointerException.class, () -> s.setHandler(null));
  }

  @Test
  void setHandlerOnlyChangesThatSave() {
    FlixelSave a = new FlixelSave(FlixelNoopSaveHandler.INSTANCE);
    FlixelSave b = new FlixelSave(FlixelNoopSaveHandler.INSTANCE);
    FlixelSaveHandler handler = new FlixelMemorySaveHandler();
    a.setHandler(handler);
    assertSame(handler, a.getHandler());
    assertSame(FlixelNoopSaveHandler.INSTANCE, b.getHandler());

    assertTrue(a.bind("flixelgdx_junit_set", null));
    a.data.put("k", 1);
    assertTrue(a.flush());
    assertTrue(handler.exists(a.getKey()));
  }
}
