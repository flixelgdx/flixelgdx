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
package org.flixelgdx.backend.lwjgl3.file;

import com.badlogic.gdx.Gdx;

import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.file.FlixelFiles;
import org.jetbrains.annotations.NotNull;

/**
 * Desktop {@link FlixelFiles} backed by libGDX's {@code Gdx.files}.
 *
 * <p>Each root forwards to the matching libGDX file type and wraps the result in a
 * {@link FlixelLwjgl3File}. The launcher installs one of these on
 * {@link org.flixelgdx.Flixel#files Flixel.files} at startup, so the shared framework reads files
 * through the seam without naming the desktop file library.
 */
public final class FlixelLwjgl3Files implements FlixelFiles {

  @Override
  @NotNull
  public FlixelFile internal(@NotNull String path) {
    return new FlixelLwjgl3File(Gdx.files.internal(path));
  }

  @Override
  @NotNull
  public FlixelFile classpath(@NotNull String path) {
    return new FlixelLwjgl3File(Gdx.files.classpath(path));
  }

  @Override
  @NotNull
  public FlixelFile external(@NotNull String path) {
    return new FlixelLwjgl3File(Gdx.files.external(path));
  }

  @Override
  @NotNull
  public FlixelFile local(@NotNull String path) {
    return new FlixelLwjgl3File(Gdx.files.local(path));
  }

  @Override
  @NotNull
  public FlixelFile absolute(@NotNull String path) {
    return new FlixelLwjgl3File(Gdx.files.absolute(path));
  }
}
