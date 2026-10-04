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
package org.flixelgdx.audio;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelHeadlessExtension;
import org.flixelgdx.asset.FlixelAssetManager;
import org.flixelgdx.asset.FlixelBaseAssetManager;
import org.flixelgdx.file.FlixelFile;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(FlixelHeadlessExtension.class)
class FlixelSoundManagerTest {

  private static final byte[] AUDIO = { 1, 2, 3, 4 };

  @TempDir
  Path tempDir;

  private FlixelAssetManager previousAssets;
  private FlixelSoundManager previousSound;
  private FlixelBaseAssetManager assets;
  private FlixelSoundManager manager;

  @BeforeEach
  void setUp() {
    previousAssets = Flixel.assets;
    previousSound = Flixel.sound;
    assets = new FlixelBaseAssetManager();
    Flixel.assets = assets;
    manager = new FlixelSoundManager(FlixelNoopSoundFactory.INSTANCE);
    Flixel.sound = manager;
  }

  @AfterEach
  void tearDown() {
    manager.destroy();
    assets.destroy();
    Flixel.assets = previousAssets;
    Flixel.sound = previousSound;
  }

  @Test
  void createReadsAbsoluteFileDirectly() throws IOException {
    Path file = tempDir.resolve("jump.ogg");
    Files.write(file, AUDIO);

    FlixelSound sound = manager.create(Flixel.files.absolute(file.toString()));

    assertNotNull(sound);
    assertSame(manager, sound.getManager());
    assertFalse(assets.isLoaded(file.toString()));
  }

  @Test
  void createReadsNonAssetFileWithSamePathDirectly() {
    assets.setFileResolver(path -> new MemoryFile(path, "internal:" + path, new byte[0]));

    FlixelSound sound = manager.create(new MemoryFile("sfx/coin.ogg", "local:sfx/coin.ogg", AUDIO));

    assertNotNull(sound);
    assertFalse(assets.isLoaded("sfx/coin.ogg"));
  }

  @Test
  void createCachesAssetFileThroughAssetManager() {
    assets.setFileResolver(path -> new MemoryFile(path, "internal:" + path, AUDIO));

    FlixelSound sound = manager.create(assets.resolveFile("sfx/coin.ogg"));

    assertNotNull(sound);
    assertTrue(assets.isLoaded("sfx/coin.ogg"));
  }

  @Test
  void playReadsAbsoluteFileDirectly() throws IOException {
    Path file = tempDir.resolve("theme.ogg");
    Files.write(file, AUDIO);

    FlixelSound sound = manager.play(Flixel.files.absolute(file.toString()));

    assertTrue(sound.isPlaying());
  }

  private record MemoryFile(@NotNull String path, @NotNull String absolutePath, byte @NotNull [] data)
      implements FlixelFile {

    @NotNull
    @Override
    public String getPath() {
      return path;
    }

    @NotNull
    @Override
    public String getAbsolutePath() {
      return absolutePath;
    }

    @Override
    public boolean exists() {
      return data.length > 0;
    }

    @Override
    public byte @NotNull [] readBytes() {
      return data;
    }
  }
}
