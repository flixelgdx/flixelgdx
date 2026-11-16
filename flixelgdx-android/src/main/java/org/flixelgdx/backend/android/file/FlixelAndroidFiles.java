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

import org.flixelgdx.backend.jvm.file.FlixelJvmFile;
import org.flixelgdx.backend.jvm.file.FlixelJvmFiles;
import org.flixelgdx.file.FlixelFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import android.content.Context;
import android.content.res.AssetManager;

/**
 * Android file system that routes reads to the APK asset manager and disk.
 *
 * <p>Root mapping:
 * <ul>
 *   <li>{@link #internal(String)} - APK {@link AssetManager} first, then classpath for
 *     framework-bundled resources (fonts, shaders). Returns a {@link FlixelAndroidAssetFile}.</li>
 *   <li>{@link #classpath(String)} - Java classpath resources.</li>
 *   <li>{@link #local(String)} - {@code Context.getFilesDir()} for private app storage.</li>
 *   <li>{@link #external(String)} - {@code Context.getExternalFilesDir(null)} when available,
 *     falls back to internal storage.</li>
 *   <li>{@link #absolute(String)} - an absolute path on the device file system.</li>
 * </ul>
 *
 * <p>Every root except {@link #internal(String)} comes from {@link FlixelJvmFiles}, which desktop
 * shares, so those roots hand out a {@link FlixelJvmFile}.
 */
public class FlixelAndroidFiles extends FlixelJvmFiles {

  @NotNull
  private final AssetManager assets;

  /**
   * Creates the file system for the given {@link Context}.
   *
   * @param context The Android context used to resolve storage directories and the asset manager.
   */
  public FlixelAndroidFiles(@NotNull Context context) {
    super(context.getFilesDir(), externalRoot(context));
    this.assets = context.getAssets();
  }

  @NotNull
  @Override
  public FlixelFile internal(@NotNull String path) {
    return new FlixelAndroidAssetFile(assets, path);
  }

  @NotNull
  private static File externalRoot(@NotNull Context context) {
    File ext = context.getExternalFilesDir(null);
    return ext != null ? ext : context.getFilesDir();
  }

  @NotNull
  public AssetManager getAssets() {
    return assets;
  }
}
