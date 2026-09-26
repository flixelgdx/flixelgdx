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
package org.flixelgdx.backend.android.file;

import android.content.Context;
import android.content.res.AssetManager;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.file.FlixelFiles;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Android file system that routes reads to the APK asset manager and disk.
 *
 * <p>Root mapping:
 * <ul>
 *   <li>{@link #internal(String)} - APK {@link AssetManager} first, then classpath for
 *     framework-bundled resources (fonts, shaders).</li>
 *   <li>{@link #classpath(String)} - Java classpath resources.</li>
 *   <li>{@link #local(String)} - {@code Context.getFilesDir()} for private app storage.</li>
 *   <li>{@link #external(String)} - {@code Context.getExternalFilesDir(null)} when available,
 *     falls back to internal storage.</li>
 *   <li>{@link #pref(String, String, String)} - a named sub-directory of {@code getFilesDir()}.</li>
 *   <li>{@link #absolute(String)} - an absolute path on the device file system.</li>
 * </ul>
 */
public class FlixelAndroidFiles implements FlixelFiles {

  @NotNull
  private final AssetManager assets;

  @NotNull
  private final File localRoot;

  @NotNull
  private final File externalRoot;

  /**
   * Creates the file system for the given {@link Context}.
   *
   * @param context The Android context used to resolve storage directories and the asset manager.
   */
  public FlixelAndroidFiles(@NotNull Context context) {
    this.assets = context.getAssets();
    this.localRoot = context.getFilesDir();
    File ext = context.getExternalFilesDir(null);
    this.externalRoot = ext != null ? ext : context.getFilesDir();
  }

  @NotNull
  @Override
  public FlixelFile internal(@NotNull String path) {
    return new FlixelAndroidFile(assets, path, null, false, true);
  }

  @NotNull
  @Override
  public FlixelFile classpath(@NotNull String path) {
    return new FlixelAndroidFile(null, path, null, true, false);
  }

  @NotNull
  @Override
  public FlixelFile local(@NotNull String path) {
    return new FlixelAndroidFile(null, path, new File(localRoot, path), false, false);
  }

  @NotNull
  @Override
  public FlixelFile external(@NotNull String path) {
    return new FlixelAndroidFile(null, path, new File(externalRoot, path), false, false);
  }

  @NotNull
  @Override
  public FlixelFile absolute(@NotNull String path) {
    return new FlixelAndroidFile(null, path, new File(path), false, false);
  }

  @NotNull
  @Override
  public FlixelFile pref(@NotNull String org, @NotNull String app, @NotNull String path) {
    File root = new File(localRoot, org + "/" + app);
    return new FlixelAndroidFile(null, path, new File(root, path), false, false);
  }
}
