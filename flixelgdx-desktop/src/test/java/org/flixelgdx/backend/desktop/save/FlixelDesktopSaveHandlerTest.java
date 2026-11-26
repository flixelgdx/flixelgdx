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
package org.flixelgdx.backend.desktop.save;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that {@link FlixelDesktopSaveHandler} stores each save as one JSON file on disk.
 *
 * <p>The save logic itself is tested in the core tests against an in-memory handler. This test only
 * covers what is specific to desktop: the file name, the folder being created, and that a write
 * leaves no temporary file behind.
 */
class FlixelDesktopSaveHandlerTest {

  @Test
  void writeStoresOneJsonFileNamedAfterTheSave(@TempDir Path dir) throws IOException {
    FlixelDesktopSaveHandler handler = new FlixelDesktopSaveHandler(dir);
    assertTrue(handler.write("MIC.settings", "{\"volume\":0.5}"));

    Path file = dir.resolve("MIC.settings.json");
    assertTrue(Files.isRegularFile(file));
    assertEquals("{\"volume\":0.5}", Files.readString(file, StandardCharsets.UTF_8));
    assertFalse(Files.exists(dir.resolve("MIC.settings.json.tmp")), "the temporary file must be moved away");
  }

  @Test
  void writeCreatesAMissingFolder(@TempDir Path dir) {
    FlixelDesktopSaveHandler handler = new FlixelDesktopSaveHandler(dir.resolve("a").resolve("b"));
    assertTrue(handler.write("slot1", "{}"));
    assertTrue(Files.isRegularFile(dir.resolve("a").resolve("b").resolve("slot1.json")));
  }

  @Test
  void readWriteAndDeleteRoundTrip(@TempDir Path dir) {
    FlixelDesktopSaveHandler handler = new FlixelDesktopSaveHandler(dir);
    assertNull(handler.read("slot1"));
    assertFalse(handler.exists("slot1"));

    assertTrue(handler.write("slot1", "first"));
    assertTrue(handler.write("slot1", "second"));
    assertEquals("second", handler.read("slot1"));
    assertTrue(handler.exists("slot1"));

    assertTrue(handler.delete("slot1"));
    assertFalse(handler.exists("slot1"));
    assertTrue(handler.delete("slot1"), "deleting a save that is already gone still succeeds");
  }
}
