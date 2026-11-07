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

class FlixelFilePickerHelpersTest {

  @Test
  void normalizeStripsDotsAndCase() {
    assertArrayEquals(new String[] { "png", "jpg", "ogg" },
        FlixelFilePickerHelpers.normalize(new String[] { ".PNG", "*.jpg", "  Ogg " }));
  }

  @Test
  void normalizeDropsBlankAndNullEntries() {
    assertArrayEquals(new String[] { "txt" }, FlixelFilePickerHelpers.normalize(new String[] { "", null, " ", "txt" }));
  }

  @Test
  void normalizeWildcardMeansAnyFile() {
    assertSame(FlixelFilePickerHelpers.NO_FILTER, FlixelFilePickerHelpers.normalize(new String[] { "png", "*" }));
    assertSame(FlixelFilePickerHelpers.NO_FILTER, FlixelFilePickerHelpers.normalize(new String[] { "*" }));
  }

  @Test
  void normalizeEmptyOrNullMeansAnyFile() {
    assertSame(FlixelFilePickerHelpers.NO_FILTER, FlixelFilePickerHelpers.normalize(null));
    assertSame(FlixelFilePickerHelpers.NO_FILTER, FlixelFilePickerHelpers.normalize(new String[0]));
    assertSame(FlixelFilePickerHelpers.NO_FILTER, FlixelFilePickerHelpers.normalize(new String[] { "", null }));
  }

  @Test
  void matchesComparesExtensionsIgnoringCase() {
    String[] exts = { "png", "jpg" };
    assertTrue(FlixelFilePickerHelpers.matches("hero.PNG", exts));
    assertTrue(FlixelFilePickerHelpers.matches("a.b.jpg", exts));
    assertFalse(FlixelFilePickerHelpers.matches("hero.gif", exts));
    assertFalse(FlixelFilePickerHelpers.matches("png", exts));
    assertFalse(FlixelFilePickerHelpers.matches("hero.", exts));
  }

  @Test
  void matchesAcceptsEverythingWithoutFilter() {
    assertTrue(FlixelFilePickerHelpers.matches("anything", FlixelFilePickerHelpers.NO_FILTER));
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

  @Test
  void withExtensionAddsFirstExtensionOnlyWhenMissing() {
    String[] exts = { "json", "txt" };
    assertEquals("level.json", FlixelFilePickerHelpers.withExtension("level", exts));
    assertEquals("/home/me/level.json", FlixelFilePickerHelpers.withExtension("/home/me/level", exts));
    assertEquals("C:\\saves.d\\level.json", FlixelFilePickerHelpers.withExtension("C:\\saves.d\\level", exts));
    assertEquals("level.txt", FlixelFilePickerHelpers.withExtension("level.txt", exts));
    assertEquals("level.png", FlixelFilePickerHelpers.withExtension("level.png", exts));
    assertEquals("level", FlixelFilePickerHelpers.withExtension("level", FlixelFilePickerHelpers.NO_FILTER));
  }

  @Test
  void unsupportedHostReportsSaveFailureImmediately() {
    FlixelHostIntegration host = FlixelNoopHostIntegration.INSTANCE;
    byte[] data = { 1, 2, 3 };

    int[] calls = new int[1];
    FlixelFileSaveListener listener = saved -> {
      assertFalse(saved);
      calls[0]++;
    };
    host.saveFile(data, "level.json");
    host.saveFile(data, "level.json", listener);
    host.saveFile(data, "level.json", listener, "json");
    host.saveFile("{}", "level.json");
    host.saveFile("{}", "level.json", listener);
    host.saveFile("{}", "level.json", listener, "json");
    assertEquals(4, calls[0]);
  }
}
