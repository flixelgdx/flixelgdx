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
package org.flixelgdx;

import org.flixelgdx.debug.ReadbackTestSupport.ReadbackGraphics;
import org.flixelgdx.graphics.FlixelNoopGraphicsManager;
import org.flixelgdx.save.FlixelNoopSaveHandler;
import org.flixelgdx.save.FlixelSave;
import org.flixelgdx.testing.FlixelMemoryFiles;
import org.flixelgdx.testing.FlixelMemorySaveHandler;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlixelHeadlessExtensionTest {

  @Test
  @Order(1)
  void aTestCanChangeGlobalsWithoutCleaningUp() {
    assertInstanceOf(FlixelMemoryFiles.class, Flixel.files, "the extension installs the in-memory file system");

    Flixel.graphics = new ReadbackGraphics();
    Flixel.cameras.add(new FlixelCamera(10, 10));
    FlixelSave.defaultHandler = new FlixelMemorySaveHandler();
    Flixel.timeScale = 3f;
    Flixel.autoPause = false;
  }

  @Test
  @Order(2)
  void theNextTestSeesTheOriginalGlobals() {
    assertSame(FlixelNoopGraphicsManager.INSTANCE, Flixel.graphics);
    assertEquals(0, Flixel.cameras.getSize());
    assertSame(FlixelNoopSaveHandler.INSTANCE, FlixelSave.defaultHandler);
    assertEquals(1f, Flixel.timeScale);
    assertTrue(Flixel.autoPause);
  }
}
