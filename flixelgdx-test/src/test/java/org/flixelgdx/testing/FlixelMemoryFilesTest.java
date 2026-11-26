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
package org.flixelgdx.testing;

import org.flixelgdx.file.FlixelFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelMemoryFilesTest {

  @Test
  void writtenFilesCanBeReadBack() {
    FlixelMemoryFiles files = new FlixelMemoryFiles();
    FlixelFile file = files.external("saves/slot1.json");
    assertFalse(file.exists());
    assertEquals("", file.readString());

    assertTrue(file.writeString("{\"score\":3}"));
    assertTrue(files.external("saves/slot1.json").exists());
    assertEquals("{\"score\":3}", files.external("saves/slot1.json").readString());
    assertEquals(11L, file.length());
    assertEquals("slot1.json", file.getName());
  }

  @Test
  void rootsDoNotShareFiles() {
    FlixelMemoryFiles files = new FlixelMemoryFiles();
    files.external("a.txt").writeString("external");
    assertFalse(files.local("a.txt").exists());
    assertFalse(files.absolute("a.txt").exists());
  }

  @Test
  void bytesAreCopiedSoCallersCannotChangeStoredData() {
    FlixelMemoryFiles files = new FlixelMemoryFiles();
    byte[] data = { 1, 2, 3 };
    files.local("data.bin").writeBytes(data);
    data[0] = 9;
    byte[] read = files.local("data.bin").readBytes();
    assertArrayEquals(new byte[] { 1, 2, 3 }, read);
    read[1] = 9;
    assertArrayEquals(new byte[] { 1, 2, 3 }, files.local("data.bin").readBytes());
  }

  @Test
  void listReturnsOnlyDirectChildren() {
    FlixelMemoryFiles files = new FlixelMemoryFiles();
    files.external("levels/one.json").writeString("1");
    files.external("levels/two.json").writeString("2");
    files.external("levels/art/tiles.png").writeBytes(new byte[] { 0 });

    FlixelFile levels = files.external("levels");
    assertTrue(levels.isDirectory());
    assertTrue(levels.exists());
    assertEquals(3, levels.list().length, "two files and the art folder");
    assertEquals(2, levels.list(".json").length);
    assertTrue(files.external("levels/art").isDirectory());
  }

  @Test
  void deleteRemovesAFileAndClearRemovesEverything() {
    FlixelMemoryFiles files = new FlixelMemoryFiles();
    files.external("a.txt").writeString("a");
    files.external("b.txt").writeString("b");
    assertTrue(files.external("a.txt").delete());
    assertFalse(files.external("a.txt").exists());
    assertFalse(files.external("a.txt").delete(), "there is nothing left to delete");

    files.clear();
    assertFalse(files.external("b.txt").exists());
  }

  @Test
  void bundledFilesComeFromTheTestClasspath() {
    FlixelMemoryFiles files = new FlixelMemoryFiles();
    FlixelFile fixture = files.internal("org/flixelgdx/debug/remote/fixtures/msg_ping.bin");
    assertTrue(fixture.exists());
    assertTrue(fixture.readBytes().length > 0);
    assertFalse(files.internal("missing.txt").exists());
    assertFalse(fixture.writeString("x"), "bundled files are read-only");
  }
}
