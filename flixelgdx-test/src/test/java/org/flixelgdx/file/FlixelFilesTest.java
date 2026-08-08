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

import org.flixelgdx.Flixel;
import org.flixelgdx.asset.FlixelAssetPaths;
import org.flixelgdx.util.FlixelPathsUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@link FlixelFiles}/{@link FlixelFile} seam: its safe no-op defaults, and that real
 * consumers ({@link FlixelPathsUtil}, {@link FlixelAssetPaths}) route their file access through
 * {@link Flixel#files}.
 */
class FlixelFilesTest {

  /** A file that only knows a path and whether it "exists", for driving consumer logic. */
  private static final class FakeFile implements FlixelFile {

    private final String path;
    private final boolean exists;

    FakeFile(String path, boolean exists) {
      this.path = path;
      this.exists = exists;
    }

    @Override
    public @NotNull String getPath() {
      return path;
    }

    @Override
    public boolean exists() {
      return exists;
    }
  }

  /** A file system whose "existing" internal paths are fixed up front. */
  private static final class FakeFiles implements FlixelFiles {

    private final String[] existingInternal;

    FakeFiles(String... existingInternal) {
      this.existingInternal = existingInternal;
    }

    @Override
    public FlixelFile internal(String path) {
      for (int i = 0; i < existingInternal.length; i++) {
        if (existingInternal[i].equals(path)) {
          return new FakeFile(path, true);
        }
      }
      return new FakeFile(path, false);
    }

    @Override
    public FlixelFile external(String path) {
      return new FakeFile(path, false);
    }
  }

  @AfterEach
  void restoreFiles() {
    Flixel.files = FlixelNoopFiles.INSTANCE;
  }

  @Test
  void noopFilesReportNothingAndNeverCrash() {
    FlixelFile file = FlixelNoopFiles.INSTANCE.internal("anything.txt");
    assertFalse(file.exists(), "The no-op file should not exist.");
    assertEquals("", file.readString(), "The no-op file should read as empty.");
    assertEquals(0, file.readBytes().length, "The no-op file should have no bytes.");
    assertNull(file.getNativeHandle(), "The no-op file has no native handle.");
    assertSame(FlixelNoopFile.INSTANCE, FlixelNoopFiles.INSTANCE.external("save.dat"));
  }

  @Test
  void pathsUtilRoutesThroughFlixelFiles() {
    Flixel.files = new FakeFiles("shared/images/logo.png");

    assertTrue(FlixelPathsUtil.sharedImageAsset("logo").exists(),
        "sharedImageAsset should resolve shared/images/<name>.png through Flixel.files.");
    assertEquals("fonts/pixel.ttf", FlixelPathsUtil.fontAsset("pixel").getPath(),
        "fontAsset should build the fonts/<name>.ttf internal path.");
    assertFalse(FlixelPathsUtil.asset("missing.png").exists(),
        "An unknown asset should report as not existing.");
  }

  @Test
  void compressedTexturePathPrefersKtx2SiblingWhenItExists() {
    Flixel.files = new FakeFiles("images/player.ktx2");

    assertEquals("images/player.ktx2", FlixelAssetPaths.resolveCompressedTexturePath("images/player.png"),
        "A .png with a .ktx2 sibling should resolve to the .ktx2 path.");
    assertEquals("images/bg.png", FlixelAssetPaths.resolveCompressedTexturePath("images/bg.png"),
        "A .png without a .ktx2 sibling should stay unchanged.");
  }

  @Test
  void compressedTexturePathFallsBackToPngWhenKtx2Missing() {
    Flixel.files = new FakeFiles("images/hero.png");

    assertEquals("images/hero.png", FlixelAssetPaths.resolveCompressedTexturePath("images/hero.ktx2"),
        "A missing .ktx2 should fall back to its .png sibling when that exists.");
  }
}
