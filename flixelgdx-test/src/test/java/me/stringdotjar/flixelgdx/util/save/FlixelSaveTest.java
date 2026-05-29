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
package me.stringdotjar.flixelgdx.util.save;

import com.badlogic.gdx.utils.ObjectMap;
import me.stringdotjar.flixelgdx.GdxHeadlessExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(GdxHeadlessExtension.class)
class FlixelSaveTest {

  @Test
  void bindFlushRoundTrip() {
    String name = "flixelgdx_junit_save_" + System.nanoTime();
    FlixelSave a = new FlixelSave();
    assertTrue(a.bind(name, null));
    a.data.put("score", 42);
    assertTrue(a.flush());

    FlixelSave b = new FlixelSave();
    assertTrue(b.bind(name, null));
    Object v = b.data.get("score");
    assertEquals(42.0, ((Number) v).doubleValue(), 1e-6);
  }

  @Test
  void mergeDataRespectsOverwrite() {
    FlixelSave s = new FlixelSave();
    assertTrue(s.bind("flixelgdx_junit_merge_" + System.nanoTime(), null));
    s.data.put("k", "a");
    ObjectMap<String, Object> in = new ObjectMap<>();
    in.put("k", "b");
    in.put("other", 1);
    s.mergeData(in, false, false);
    assertEquals("a", s.data.get("k"));
    s.mergeData(in, true, false);
    assertEquals("b", s.data.get("k"));
  }
}
