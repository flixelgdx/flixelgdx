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

import org.flixelgdx.asset.FlixelAssetManager;
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
   * Returns the absolute file system path for this handle.
   *
   * <p>For handles backed by a real file on disk (such as those from {@link FlixelFiles#external(String)}
   * or {@link FlixelFiles#absolute(String)}), this is the full path as the OS would see it. For
   * classpath or no-op handles it falls back to {@link #getPath()}.
   *
   * @return The absolute path, or the relative path as a fallback.
   */
  @NotNull
  default String getAbsolutePath() {
    return getPath();
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
   * Returns {@code true} when a readable file exists at this location. Defaults to {@code false}.
   */
  default boolean exists() {
    return false;
  }

  /**
   * Returns {@code true} when this location is a directory rather than a file. Defaults to
   * {@code false}.
   */
  default boolean isDirectory() {
    return false;
  }

  /**
   * Lists the immediate children of this handle when it points at a directory.
   *
   * <p>Only the direct children are returned, not the whole tree. The order is not guaranteed. When
   * this handle is not a directory, or the backend cannot enumerate it (for example a directory
   * packed inside a JAR, which most backends cannot walk), an empty array is returned rather than
   * {@code null}.
   *
   * <p>Example:
   *
   * <pre>{@code
   * FlixelFile[] levels = Flixel.files.internal("levels").list();
   * for (int i = 0; i < levels.length; i++) {
   *   loadLevel(levels[i]);
   * }
   * }</pre>
   *
   * @return The child handles, or an empty array when there are none. Never {@code null}.
   */
  @NotNull
  default FlixelFile[] list() {
    return new FlixelFile[0];
  }

  /**
   * Lists the immediate children whose file name ends with the given suffix.
   *
   * <p>This is a convenience filter over {@link #list()}. The suffix is matched against the child's
   * file name, so passing {@code "png"} keeps every child whose name ends with {@code png} (such as
   * {@code hero.png}). The same empty-array rules as {@link #list()} apply.
   *
   * @param suffix The file-name suffix to keep, for example {@code "png"} or {@code ".ogg"}.
   * @return The matching child handles, or an empty array when there are none. Never {@code null}.
   */
  @NotNull
  default FlixelFile[] list(@NotNull String suffix) {
    return new FlixelFile[0];
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
   * <p>On most platforms this returns decoded, ready-to-use content. On restricted platforms (such
   * as web) it returns the raw encoded bytes exactly as they were stored on the server. In
   * particular, image files on web return raw PNG or JPEG bytes, not decoded RGBA pixels. Code that
   * expects decoded pixel data should load images through {@link FlixelAssetManager} instead of calling
   * this method directly, so the platform's async decode path runs properly. Games targeting web should
   * preload all images through {@code Flixel.assets.load()} and {@code Flixel.assets.update()}.
   *
   * @return The file contents, or an empty array when the file does not exist. Never {@code null}.
   */
  @NotNull
  default byte[] readBytes() {
    return new byte[0];
  }

  /**
   * Returns the size of this file in bytes.
   *
   * @return The file length, or {@code 0} when the file does not exist or the backend cannot
   *     report sizes.
   */
  default long length() {
    return 0L;
  }

  /**
   * Writes text to this file, replacing any existing contents. Parent folders are created as
   * needed on backends that support writing.
   *
   * <p>Only writable roots (such as {@link FlixelFiles#external external} and
   * {@link FlixelFiles#local local}) support this; read-only roots return {@code false}.
   *
   * @param content The text to write.
   * @return {@code true} when the write succeeded.
   */
  default boolean writeString(@NotNull String content) {
    return false;
  }

  /**
   * Writes raw bytes to this file, replacing any existing contents. Parent folders are created
   * as needed on backends that support writing.
   *
   * @param content The bytes to write.
   * @return {@code true} when the write succeeded.
   */
  default boolean writeBytes(byte @NotNull [] content) {
    return false;
  }

  /**
   * Deletes this file.
   *
   * @return {@code true} when the file existed and was removed.
   */
  default boolean delete() {
    return false;
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
