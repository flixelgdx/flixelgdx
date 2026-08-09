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

import com.badlogic.gdx.files.FileHandle;

import org.flixelgdx.file.FlixelFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Desktop {@link FlixelFile} backed by a libGDX {@link FileHandle}.
 *
 * <p>Each read simply forwards to the wrapped handle, so this is a thin adapter that lets the shared
 * framework read files without depending on the desktop file library directly. Instances come from
 * {@link FlixelLwjgl3Files}; game code does not construct them by hand.
 */
public final class FlixelLwjgl3File implements FlixelFile {

  private final FileHandle handle;

  /**
   * Wraps a libGDX file handle.
   *
   * @param handle The handle to delegate to. Must not be {@code null}.
   */
  public FlixelLwjgl3File(@NotNull FileHandle handle) {
    this.handle = handle;
  }

  @Override
  @NotNull
  public String getPath() {
    return handle.path();
  }

  @Override
  @NotNull
  public String getName() {
    return handle.name();
  }

  @Override
  public boolean exists() {
    return handle.exists();
  }

  @Override
  public boolean isDirectory() {
    return handle.isDirectory();
  }

  @Override
  @NotNull
  public String readString() {
    return handle.readString();
  }

  @Override
  @NotNull
  public String readString(@Nullable String charset) {
    return charset == null ? handle.readString() : handle.readString(charset);
  }

  @Override
  @NotNull
  public byte[] readBytes() {
    return handle.readBytes();
  }

  @Override
  @NotNull
  public Object getNativeHandle() {
    return handle;
  }
}
