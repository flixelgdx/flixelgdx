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
import org.flixelgdx.file.FlixelFiles;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * The desktop file system.
 *
 * <p>Each root maps to a real directory on disk (or the classpath):
 * <ul>
 *   <li>{@link #internal(String)} - the {@code assets/} folder next to the working directory,
 *     the standard place bundled game assets live during development. Falls back to the classpath
 *     when the on-disk file is missing, so a packaged JAR still finds its assets.</li>
 *   <li>{@link #classpath(String)} - resources on the Java classpath.</li>
 *   <li>{@link #external(String)} - the user's home directory, for save data.</li>
 *   <li>{@link #local(String)} - the working directory.</li>
 *   <li>{@link #absolute(String)} - an absolute filesystem path.</li>
 * </ul>
 */
public final class FlixelJvmFiles implements FlixelFiles {

  @NotNull
  private final String externalRoot = System.getProperty("user.home", ".");

  @NotNull
  private final String localRoot = System.getProperty("user.dir", ".");

  @NotNull
  @Override
  public FlixelFile internal(@NotNull String path) {
    File onDisk = new File(localRoot, "assets/" + path);
    if (onDisk.exists()) {
      return new FlixelJvmFile(path, onDisk, false);
    }
    File direct = new File(localRoot, path);
    if (direct.exists()) {
      return new FlixelJvmFile(path, direct, false);
    }
    // Fall back to the classpath so packaged games still resolve their assets.
    return new FlixelJvmFile(path, null, true);
  }

  @NotNull
  @Override
  public FlixelFile classpath(@NotNull String path) {
    return new FlixelJvmFile(path, null, true);
  }

  @NotNull
  @Override
  public FlixelFile external(@NotNull String path) {
    return new FlixelJvmFile(path, new File(externalRoot, path), false);
  }

  @NotNull
  @Override
  public FlixelFile local(@NotNull String path) {
    return new FlixelJvmFile(path, new File(localRoot, path), false);
  }

  @NotNull
  @Override
  public FlixelFile absolute(@NotNull String path) {
    return new FlixelJvmFile(path, new File(path), false);
  }
}
