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

import org.flixelgdx.file.FlixelFile;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * The raw bytes of an encoded audio file, plus the path they came from.
 *
 * <p>This is what the asset pipeline produces for audio: reading the file happens on a worker
 * thread through the {@link FlixelFile} seam, and the backend decodes the bytes from memory when
 * a {@link FlixelSound} is created. Keeping audio in memory this way means sounds work
 * identically from a folder on disk, a packaged JAR, or any other file root, with no
 * temp-file extraction or string-path tricks.
 */
public final class FlixelSoundBuffer {

  @NotNull
  private final String path;

  private final byte @NotNull [] data;

  /**
   * Wraps encoded audio bytes.
   *
   * @param path The asset path the bytes came from, for diagnostics.
   * @param data The encoded file contents.
   */
  public FlixelSoundBuffer(@NotNull String path, byte @NotNull [] data) {
    this.path = Objects.requireNonNull(path, "path cannot be null.");
    this.data = Objects.requireNonNull(data, "data cannot be null.");
  }

  /**
   * Reads a file into a buffer.
   *
   * @param path The asset path, for diagnostics.
   * @param file The file to read.
   * @return A buffer with the file's contents.
   * @throws IllegalStateException If the file does not exist or is empty.
   */
  @NotNull
  public static FlixelSoundBuffer read(@NotNull String path, @NotNull FlixelFile file) {
    byte[] bytes = file.readBytes();
    if (bytes.length == 0) {
      throw new IllegalStateException("Audio file not found or empty: '" + path + "'.");
    }
    return new FlixelSoundBuffer(path, bytes);
  }

  @NotNull
  public String getPath() {
    return path;
  }

  /** Returns the encoded file bytes. Treat as read-only. */
  public byte @NotNull [] getData() {
    return data;
  }
}
