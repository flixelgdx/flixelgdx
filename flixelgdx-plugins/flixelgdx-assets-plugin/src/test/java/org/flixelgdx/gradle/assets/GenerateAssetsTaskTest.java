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
package org.flixelgdx.gradle.assets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateAssetsTaskTest {

  @TempDir
  Path dir;

  @Test
  void writesTheClassUnderItsPackageFolder() throws IOException {
    Path assets = dir.resolve("assets");
    Path out = dir.resolve("out");
    Path player = touch(assets.resolve("shared/sprites/player.png"));

    GenerateAssetsTask.generate(assets.toFile(), List.of(player.toFile()), "com.mygame", "Assets", out.toFile());

    String source = Files.readString(out.resolve("com/mygame/Assets.java"));
    assertTrue(source.contains("PLAYER_PNG = \"shared/sprites/player.png\""), source);
  }

  @Test
  void identicalOutputIsNotRewritten() throws IOException {
    Path assets = dir.resolve("assets");
    Path out = dir.resolve("out");
    Path file = touch(assets.resolve("a.png"));
    GenerateAssetsTask.generate(assets.toFile(), List.of(file.toFile()), "", "Assets", out.toFile());
    Path generated = out.resolve("Assets.java");
    FileTime old = FileTime.fromMillis(1_000_000L);
    Files.setLastModifiedTime(generated, old);

    GenerateAssetsTask.generate(assets.toFile(), List.of(file.toFile()), "", "Assets", out.toFile());

    assertEquals(old, Files.getLastModifiedTime(generated));
  }

  @Test
  void oldFilesAreRemovedWhenThePackageChanges() throws IOException {
    Path assets = dir.resolve("assets");
    Path out = dir.resolve("out");
    Path file = touch(assets.resolve("a.png"));
    GenerateAssetsTask.generate(assets.toFile(), List.of(file.toFile()), "com.old", "Assets", out.toFile());

    GenerateAssetsTask.generate(assets.toFile(), List.of(file.toFile()), "com.fresh", "Assets", out.toFile());

    assertTrue(Files.isRegularFile(out.resolve("com/fresh/Assets.java")));
    assertFalse(Files.exists(out.resolve("com/old")));
  }

  private static Path touch(Path file) throws IOException {
    Files.createDirectories(file.getParent());
    return Files.writeString(file, "x");
  }
}
