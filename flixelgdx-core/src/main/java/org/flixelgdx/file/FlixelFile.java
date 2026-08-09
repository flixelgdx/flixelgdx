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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A single file or resource that FlixelGDX can read, without naming a file library.
 *
 * <p>A handle is a lightweight pointer to a location; it does not open or read anything until you
 * ask. You obtain one from {@link FlixelFiles} (for example {@code Flixel.files.internal("data.json")})
 * and then query it or read its contents.
 *
 * <p>Every method has a safe default so a backend only needs to override what it actually supports,
 * and so the no-op handle ({@link FlixelNoopFile}) can report "nothing here" on headless sessions and
 * platforms without a file backend yet. In that state {@link #exists()} is {@code false} and the read
 * methods return empty content rather than throwing, so callers stay simple.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelFile file = Flixel.files.internal("levels/level1.json");
 * if (file.exists()) {
 *   String json = file.readString();
 *   // parse json...
 * }
 * }</pre>
 *
 * @see FlixelFiles
 */
public interface FlixelFile {

  /**
   * Returns the path this handle points at, using forward slashes.
   *
   * @return The path, or an empty string when this handle points at nothing.
   */
  @NotNull
  default String getPath() {
    return "";
  }

  /**
   * Returns the file name (the last path segment), including its extension.
   *
   * @return The file name, or an empty string when this handle points at nothing.
   */
  @NotNull
  default String getName() {
    return "";
  }

  /**
   * @return {@code true} when a readable file exists at this location. Defaults to {@code false}.
   */
  default boolean exists() {
    return false;
  }

  /**
   * @return {@code true} when this location is a directory rather than a file. Defaults to
   *     {@code false}.
   */
  default boolean isDirectory() {
    return false;
  }

  /**
   * Reads the whole file as text using the platform's default charset.
   *
   * @return The file contents, or an empty string when the file does not exist. Never {@code null}.
   */
  @NotNull
  default String readString() {
    return "";
  }

  /**
   * Reads the whole file as text using the named charset.
   *
   * @param charset A charset name such as {@code "UTF-8"}; the default charset is used when
   *     {@code null}.
   * @return The file contents, or an empty string when the file does not exist. Never {@code null}.
   */
  @NotNull
  default String readString(@Nullable String charset) {
    return readString();
  }

  /**
   * Reads the whole file as raw bytes.
   *
   * @return The file contents, or an empty array when the file does not exist. Never {@code null}.
   */
  @NotNull
  default byte[] readBytes() {
    return new byte[0];
  }

  /**
   * Returns the backend's native file object for advanced interop, or {@code null} when there is
   * none.
   *
   * <p>This is a deliberate escape hatch for framework code that must hand a location to a native
   * loader (for example a texture or sound loader) during the move off a single file library. It is
   * intentionally typed as {@link Object} so this interface never depends on a specific backend.
   * Game code should not need it; prefer the read methods above.
   *
   * @return The native file object, or {@code null} when unavailable.
   */
  @Nullable
  default Object getNativeHandle() {
    return null;
  }
}
