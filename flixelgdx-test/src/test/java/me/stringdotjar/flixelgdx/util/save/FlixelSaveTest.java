/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

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
    a.getData().put("score", 42);
    assertTrue(a.flush());

    FlixelSave b = new FlixelSave();
    assertTrue(b.bind(name, null));
    Object v = b.getData().get("score");
    assertEquals(42.0, ((Number) v).doubleValue(), 1e-6);
  }

  @Test
  void mergeDataRespectsOverwrite() {
    FlixelSave s = new FlixelSave();
    assertTrue(s.bind("flixelgdx_junit_merge_" + System.nanoTime(), null));
    s.getData().put("k", "a");
    ObjectMap<String, Object> in = new ObjectMap<>();
    in.put("k", "b");
    in.put("other", 1);
    s.mergeData(in, false, false);
    assertEquals("a", s.getData().get("k"));
    s.mergeData(in, true, false);
    assertEquals("b", s.getData().get("k"));
  }
}
