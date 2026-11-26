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
package org.flixelgdx.backend.jvm.file;

import org.flixelgdx.file.FlixelFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelJvmFileTest {

  private static final String FIXTURE = "org/flixelgdx/debug/remote/fixtures/msg_ping.bin";

  private static FlixelJvmFiles files(Path dir) {
    return new FlixelJvmFiles(dir.resolve("local").toFile(), dir.resolve("home").toFile()) {
    };
  }

  @Test
  void writesReadsAndDeletesRealFiles(@TempDir Path dir) {
    FlixelJvmFiles files = files(dir);
    FlixelFile f = files.external("saves/slot1.txt");
    assertFalse(f.exists());
    assertEquals(0L, f.length());
    assertEquals(0, f.readBytes().length);
    assertTrue(f.writeString("héllo"));
    assertTrue(f.exists());
    assertFalse(f.isDirectory());
    assertEquals("héllo", f.readString());
    assertEquals("héllo", f.readString("UTF-8"));
    assertEquals("héllo", f.readString(null));
    assertEquals(6L, f.length());
    assertEquals("slot1.txt", f.getName());
    assertEquals("saves/slot1.txt", f.getPath());
    assertTrue(f.getAbsolutePath().endsWith("slot1.txt"));
    assertNotNull(f.getNativeHandle());
    assertTrue(f.delete());
    assertFalse(f.delete());

    assertEquals(dir.resolve("local").toFile(), files.getLocalRoot());
    assertEquals(dir.resolve("home").toFile(), files.getExternalRoot());
    assertTrue(files.local("a.bin").writeBytes(new byte[] { 1, 2 }));
    assertArrayEquals(new byte[] { 1, 2 }, files.local("a.bin").readBytes());
    assertTrue(files.absolute(dir.resolve("abs.txt").toString()).writeString("x"));
  }

  @Test
  void listsDirectories(@TempDir Path dir) {
    FlixelJvmFiles files = files(dir);
    files.local("levels/one.json").writeString("1");
    files.local("levels/two.json").writeString("2");
    files.local("levels/art.png").writeString("3");
    FlixelFile levels = files.local("levels");
    assertTrue(levels.isDirectory());
    assertEquals(0, levels.readBytes().length, "a folder has no bytes");
    assertEquals(3, levels.list().length);
    FlixelFile[] json = levels.list(".json");
    assertEquals(2, json.length);
    assertTrue(json[0].getPath().startsWith("levels/"));
    assertEquals(0, files.local("levels/one.json").list().length, "a file has no children");
    assertEquals(0, files.local("levels/one.json").list(".json").length);

    FlixelFile slash = new FlixelJvmFile("levels/", dir.resolve("local/levels").toFile(), false);
    assertTrue(slash.list()[0].getPath().startsWith("levels/") && !slash.list()[0].getPath().contains("//"));
    FlixelFile root = new FlixelJvmFile("", dir.resolve("local/levels").toFile(), false);
    assertFalse(root.list()[0].getPath().contains("/"));
  }

  @Test
  void classpathFilesAreReadOnly() {
    FlixelJvmFiles files = files(Path.of("."));
    FlixelFile f = files.classpath(FIXTURE);
    assertTrue(f.exists());
    assertTrue(f.readBytes().length > 0);
    assertEquals(f.readBytes().length, f.length());
    assertFalse(f.isDirectory());
    assertFalse(f.writeString("x"));
    assertFalse(f.delete());
    assertEquals(0, f.list().length);
    assertEquals(FIXTURE, f.getAbsolutePath(), "a resource has no file system path");
    assertNull(f.getNativeHandle());
    assertTrue(((FlixelJvmFile) f).isClasspath());
    assertNull(((FlixelJvmFile) f).getFile());

    FlixelFile missing = files.classpath("missing/nothing.txt");
    assertFalse(missing.exists());
    assertEquals(0, missing.readBytes().length);
    assertNotNull(FlixelJvmFile.openResource("/" + FIXTURE));
  }

  @Test
  void aHandleWithoutAFileIsEmpty() {
    FlixelJvmFile f = new FlixelJvmFile("nothing\\here.txt", null, false);
    assertFalse(f.exists());
    assertEquals(0L, f.length());
    assertFalse(f.writeBytes(new byte[1]));
    assertFalse(f.delete());
    assertEquals("here.txt", f.getName(), "backslashes split names too");
    assertEquals("nothing\\here.txt", f.getAbsolutePath());
    assertEquals("plain", new FlixelJvmFile("plain", null, false).getName());
  }

  @Test
  void writingIntoAFileAsIfItWereAFolderFails(@TempDir Path dir) throws IOException {
    File blocker = dir.resolve("blocker").toFile();
    assertTrue(blocker.createNewFile());
    FlixelJvmFile f = new FlixelJvmFile("blocker/inner.txt", new File(blocker, "inner.txt"), false);
    assertFalse(f.writeString("x"));
  }

  @Test
  void readAllCopiesAWholeStream() throws IOException {
    byte[] data = new byte[20000];
    data[19999] = 7;
    byte[] read = FlixelJvmFile.readAll(new ByteArrayInputStream(data));
    assertEquals(20000, read.length);
    assertEquals(7, read[19999]);
    assertEquals(0, FlixelJvmFile.readAll(new ByteArrayInputStream(new byte[0])).length);
  }
}
