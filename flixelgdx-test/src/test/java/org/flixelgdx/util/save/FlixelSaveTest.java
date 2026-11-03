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
package org.flixelgdx.util.save;

import org.flixelgdx.FlixelHeadlessExtension;
import org.flixelgdx.backend.desktop.save.FlixelJvmSaveHandler;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.save.FlixelNoopSaveHandler;
import org.flixelgdx.save.FlixelSave;
import org.flixelgdx.save.FlixelSaveHandler;
import org.flixelgdx.save.FlixelSaveStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(FlixelHeadlessExtension.class)
class FlixelSaveTest {

  @Test
  void bindFlushRoundTrip(@TempDir Path tempDir) {
    FlixelSaveHandler handler = new FlixelJvmSaveHandler(tempDir);
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
  void nestedJsonStringRoundTrips(@TempDir Path tempDir) {
    FlixelSaveHandler handler = new FlixelJvmSaveHandler(tempDir);
    String json = "{\"startFullscreen\":true,\"masterVolume\":0.5}";
    FlixelSave a = new FlixelSave(handler);
    assertTrue(a.bind("MIC", "settings"));
    a.data.put("settings", json);
    assertTrue(a.flush());
    assertTrue(Files.isRegularFile(tempDir.resolve("MIC.settings.json")));

    FlixelSave b = new FlixelSave(handler);
    assertTrue(b.bind("MIC", "settings"));
    assertEquals(json, b.getString("settings", null));
  }

  @Test
  void eraseRemovesStoredSave(@TempDir Path tempDir) {
    FlixelSaveHandler handler = new FlixelJvmSaveHandler(tempDir);
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
  void mergeDataRespectsOverwrite(@TempDir Path tempDir) {
    FlixelSave s = new FlixelSave(new FlixelJvmSaveHandler(tempDir));
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
  void newSavesUseDefaultHandler(@TempDir Path tempDir) {
    FlixelSaveHandler previous = FlixelSave.defaultHandler;
    try {
      FlixelSaveHandler handler = new FlixelJvmSaveHandler(tempDir);
      FlixelSave.defaultHandler = handler;
      assertSame(handler, new FlixelSave().getHandler());
    } finally {
      FlixelSave.defaultHandler = previous;
    }
  }

  @Test
  void nullHandlersAreRejected(@TempDir Path tempDir) {
    FlixelSaveHandler handler = new FlixelJvmSaveHandler(tempDir);
    FlixelSave s = new FlixelSave(handler);
    assertThrows(NullPointerException.class, () -> s.setHandler(null));
  }

  @Test
  void setHandlerOnlyChangesThatSave(@TempDir Path tempDir) {
    FlixelSave a = new FlixelSave(FlixelNoopSaveHandler.INSTANCE);
    FlixelSave b = new FlixelSave(FlixelNoopSaveHandler.INSTANCE);
    FlixelSaveHandler handler = new FlixelJvmSaveHandler(tempDir);
    a.setHandler(handler);
    assertSame(handler, a.getHandler());
    assertSame(FlixelNoopSaveHandler.INSTANCE, b.getHandler());

    assertTrue(a.bind("flixelgdx_junit_set", null));
    a.data.put("k", 1);
    assertTrue(a.flush());
    assertTrue(handler.exists(a.getKey()));
  }
}
