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

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelHeadlessExtension;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.file.FlixelFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(FlixelHeadlessExtension.class)
class FlixelSaveTest {

  @Test
  void bindFlushRoundTrip(@TempDir Path tempDir) {
    FlixelFile dir = Flixel.files.absolute(tempDir.toString());
    String name = "flixelgdx_junit_save_" + System.nanoTime();
    FlixelSave a = new FlixelSave();
    assertTrue(a.bind(name, null, dir));
    a.data.put("score", 42);
    assertTrue(a.flush());

    FlixelSave b = new FlixelSave();
    assertTrue(b.bind(name, null, dir));
    Object v = b.data.get("score");
    assertEquals(42.0, ((Number) v).doubleValue(), 1e-6);
  }

  @Test
  void mergeDataRespectsOverwrite(@TempDir Path tempDir) {
    FlixelFile dir = Flixel.files.absolute(tempDir.toString());
    FlixelSave s = new FlixelSave();
    assertTrue(s.bind("flixelgdx_junit_merge_" + System.nanoTime(), null, dir));
    s.data.put("k", "a");
    FlixelMap<String, Object> in = new FlixelMap<>();
    in.put("k", "b");
    in.put("other", 1);
    s.mergeData(in, false, false);
    assertEquals("a", s.data.get("k"));
    s.mergeData(in, true, false);
    assertEquals("b", s.data.get("k"));
  }
}
