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
package org.flixelgdx.file;

import org.flixelgdx.backend.FlixelHostIntegration;
import org.flixelgdx.backend.FlixelNoopHostIntegration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelFilePickerTest {

  @Test
  void normalizeStripsDotsAndCase() {
    assertArrayEquals(new String[] { "png", "jpg", "ogg" },
        FlixelFilePicker.normalize(new String[] { ".PNG", "*.jpg", "  Ogg " }));
  }

  @Test
  void normalizeDropsBlankAndNullEntries() {
    assertArrayEquals(new String[] { "txt" }, FlixelFilePicker.normalize(new String[] { "", null, " ", "txt" }));
  }

  @Test
  void normalizeWildcardMeansAnyFile() {
    assertSame(FlixelFilePicker.NO_FILTER, FlixelFilePicker.normalize(new String[] { "png", "*" }));
    assertSame(FlixelFilePicker.NO_FILTER, FlixelFilePicker.normalize(new String[] { "*" }));
  }

  @Test
  void normalizeEmptyOrNullMeansAnyFile() {
    assertSame(FlixelFilePicker.NO_FILTER, FlixelFilePicker.normalize(null));
    assertSame(FlixelFilePicker.NO_FILTER, FlixelFilePicker.normalize(new String[0]));
    assertSame(FlixelFilePicker.NO_FILTER, FlixelFilePicker.normalize(new String[] { "", null }));
  }

  @Test
  void matchesComparesExtensionsIgnoringCase() {
    String[] exts = { "png", "jpg" };
    assertTrue(FlixelFilePicker.matches("hero.PNG", exts));
    assertTrue(FlixelFilePicker.matches("a.b.jpg", exts));
    assertFalse(FlixelFilePicker.matches("hero.gif", exts));
    assertFalse(FlixelFilePicker.matches("png", exts));
    assertFalse(FlixelFilePicker.matches("hero.", exts));
  }

  @Test
  void matchesAcceptsEverythingWithoutFilter() {
    assertTrue(FlixelFilePicker.matches("anything", FlixelFilePicker.NO_FILTER));
  }

  @Test
  void unsupportedHostDeliversEmptyArrayImmediately() {
    FlixelHostIntegration host = FlixelNoopHostIntegration.INSTANCE;
    assertFalse(host.supportsFilePicker());

    int[] calls = new int[1];
    FlixelFilePickListener listener = files -> {
      assertNotNull(files);
      assertEquals(0, files.length);
      calls[0]++;
    };
    host.pickFile(listener);
    host.pickFile(listener, "png");
    host.pickFiles(listener);
    host.pickFiles(listener, "png", "jpg");
    assertEquals(4, calls[0]);
  }
}
