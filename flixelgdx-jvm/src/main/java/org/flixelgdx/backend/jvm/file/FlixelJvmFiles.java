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
 * The file system shared by every JVM backend (desktop and Android).
 *
 * <p>Four of the five roots work the same way on every JVM platform, so this class implements
 * them once with {@link FlixelJvmFile}:
 * <ul>
 *   <li>{@link #classpath(String)} - resources on the Java classpath.</li>
 *   <li>{@link #local(String)} - files under the local root given to the constructor.</li>
 *   <li>{@link #external(String)} - files under the external root given to the constructor.</li>
 *   <li>{@link #absolute(String)} - an absolute file system path.</li>
 * </ul>
 *
 * <p>Only {@link #internal(String)} differs, because each platform ships its bundled assets in
 * a different place (a folder on disk, a JAR, or an APK). A platform subclass passes its two
 * roots to the constructor and implements that one method.
 */
public abstract class FlixelJvmFiles implements FlixelFiles {

  @NotNull
  private final File localRoot;

  @NotNull
  private final File externalRoot;

  /**
   * Creates the file system with the given roots.
   *
   * @param localRoot The directory {@link #local(String)} paths are resolved against.
   * @param externalRoot The directory {@link #external(String)} paths are resolved against.
   */
  protected FlixelJvmFiles(@NotNull File localRoot, @NotNull File externalRoot) {
    this.localRoot = localRoot;
    this.externalRoot = externalRoot;
  }

  @NotNull
  @Override
  public FlixelFile classpath(@NotNull String path) {
    return new FlixelJvmFile(path, null, true);
  }

  @NotNull
  @Override
  public FlixelFile local(@NotNull String path) {
    return new FlixelJvmFile(path, new File(localRoot, path), false);
  }

  @NotNull
  @Override
  public FlixelFile external(@NotNull String path) {
    return new FlixelJvmFile(path, new File(externalRoot, path), false);
  }

  @NotNull
  @Override
  public FlixelFile absolute(@NotNull String path) {
    return new FlixelJvmFile(path, new File(path), false);
  }

  @NotNull
  public File getLocalRoot() {
    return localRoot;
  }

  @NotNull
  public File getExternalRoot() {
    return externalRoot;
  }
}
