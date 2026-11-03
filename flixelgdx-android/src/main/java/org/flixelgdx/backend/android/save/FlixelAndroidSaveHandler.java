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
package org.flixelgdx.backend.android.save;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelConfig;
import org.flixelgdx.logging.FlixelLogger;
import org.flixelgdx.save.FlixelSaveHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import android.content.Context;
import android.util.AtomicFile;

/**
 * The Android save storage: one JSON file per save inside the app's private storage.
 *
 * <p>By default saves live in {@code Context.getFilesDir()/Company/Title/saves/}, using the company
 * name and game title set in {@link FlixelConfig}. That folder is private to the app, needs no
 * storage permission, and is removed when the app is uninstalled. Pass a folder to
 * {@link #FlixelAndroidSaveHandler(File)} to keep saves somewhere else.
 *
 * <p>Writes go through {@link AtomicFile}, Android's tool for crash-safe files: the old save is kept
 * as a backup until the new one is fully written, so the app being killed mid-save never loses it.
 */
public class FlixelAndroidSaveHandler implements FlixelSaveHandler {

  private static final FlixelLogger LOG = Flixel.log.tagged("Save");

  /** The app's private files folder, used to build the default save folder. */
  @Nullable
  private final File filesRoot;

  /** The folder saves live in, or {@code null} to use the default folder under {@link #filesRoot}. */
  @Nullable
  private final File directory;

  /**
   * Creates a handler that keeps saves in the app's private storage.
   *
   * @param context Any context of the app; only its private files folder is kept.
   */
  public FlixelAndroidSaveHandler(@NotNull Context context) {
    this.filesRoot = context.getFilesDir();
    this.directory = null;
  }

  /**
   * Creates a handler that keeps saves in a specific folder. The folder is created on the first
   * write if it does not exist yet.
   *
   * @param directory The folder to keep save files in.
   */
  public FlixelAndroidSaveHandler(@NotNull File directory) {
    this.filesRoot = null;
    this.directory = Objects.requireNonNull(directory, "directory cannot be null");
  }

  @Override
  @Nullable
  public String read(@NotNull String name) {
    AtomicFile file = new AtomicFile(fileFor(name));
    try {
      return new String(file.readFully(), StandardCharsets.UTF_8);
    } catch (FileNotFoundException e) {
      return null;
    } catch (IOException | RuntimeException e) {
      LOG.error("Could not read save file '{}'.", file.getBaseFile(), e);
      return null;
    }
  }

  @Override
  public boolean write(@NotNull String name, @NotNull String data) {
    File target = fileFor(name);
    File parent = target.getParentFile();
    if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
      LOG.error("Could not create save folder '{}'.", parent);
      return false;
    }
    AtomicFile file = new AtomicFile(target);
    FileOutputStream out = null;
    try {
      out = file.startWrite();
      out.write(data.getBytes(StandardCharsets.UTF_8));
      file.finishWrite(out);
      return true;
    } catch (IOException | RuntimeException e) {
      if (out != null) {
        file.failWrite(out);
      }
      LOG.error("Could not write save file '{}'.", target, e);
      return false;
    }
  }

  @Override
  public boolean delete(@NotNull String name) {
    new AtomicFile(fileFor(name)).delete();
    return true;
  }

  /**
   * Returns the file a save name is stored in.
   *
   * @param name The save name.
   * @return The save file inside {@link #getDirectory()}.
   */
  @NotNull
  public File fileFor(@NotNull String name) {
    return new File(getDirectory(), name + ".json");
  }

  /**
   * Returns the folder saves are kept in: the folder given to the constructor, or
   * {@code getFilesDir()/Company/Title/saves} for the current {@link Flixel#config}.
   *
   * @return The save folder; it may not exist yet.
   */
  @NotNull
  public File getDirectory() {
    if (directory != null) {
      return directory;
    }
    FlixelConfig config = Flixel.config;
    File app = new File(new File(filesRoot, config.getCompany()), config.getTitle());
    return new File(app, "saves");
  }
}
