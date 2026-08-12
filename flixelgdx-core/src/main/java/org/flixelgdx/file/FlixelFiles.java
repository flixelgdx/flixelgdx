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

/**
 * The platform's file system: how FlixelGDX opens files from the well-known roots a game uses,
 * without naming a file library.
 *
 * <p>Each method returns a {@link FlixelFile} handle that points at a location. The handle is cheap
 * and does not touch the disk until you read from it or ask whether it {@link FlixelFile#exists()}.
 * The roots mirror the standard places a game keeps data:
 *
 * <ul>
 *   <li>{@link #internal(String)} - assets bundled with the game (the {@code assets/} folder). This
 *       is what most game code wants for images, sounds, fonts, and data files.</li>
 *   <li>{@link #classpath(String)} - resources packaged on the Java classpath. Used as a fallback on
 *       desktop, where running from a JAR embeds assets as classpath resources.</li>
 *   <li>{@link #external(String)} - files under the user's home directory, for save data and other
 *       content that outlives a single run.</li>
 *   <li>{@link #local(String)} - files relative to where the application was started.</li>
 *   <li>{@link #absolute(String)} - a file named by its full path on the underlying file system.</li>
 * </ul>
 *
 * <p>Access it through {@link org.flixelgdx.Flixel#files Flixel.files}. The active backend is
 * installed there before {@link org.flixelgdx.Flixel#start(org.flixelgdx.FlixelGame, org.flixelgdx.backend.FlixelGameRunner) Flixel.start(...)}. Until then, and
 * on headless sessions, a safe default ({@link FlixelNoopFiles}) hands back empty handles so reads
 * never crash.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelFile save = Flixel.files.external("mygame/progress.sav");
 * if (save.exists()) {
 *   String data = save.readString();
 * }
 * }</pre>
 *
 * @see FlixelFile
 */
public interface FlixelFiles {

  /**
   * Opens a handle to a bundled asset (the {@code assets/} folder).
   *
   * @param path Asset path, using forward slashes (e.g. {@code "images/player.png"}).
   * @return A handle to the asset; never {@code null}.
   */
  @NotNull
  default FlixelFile internal(@NotNull String path) {
    return FlixelNoopFile.INSTANCE;
  }

  /**
   * Opens a handle to a classpath resource.
   *
   * @param path Resource path on the classpath, using forward slashes.
   * @return A handle to the resource; never {@code null}.
   */
  @NotNull
  default FlixelFile classpath(@NotNull String path) {
    return FlixelNoopFile.INSTANCE;
  }

  /**
   * Opens a handle to a file under the user's home directory, used for save data.
   *
   * @param path Path relative to the external root, using forward slashes.
   * @return A handle to the file; never {@code null}.
   */
  @NotNull
  default FlixelFile external(@NotNull String path) {
    return FlixelNoopFile.INSTANCE;
  }

  /**
   * Opens a handle to a file relative to where the application was started.
   *
   * @param path Path relative to the working directory, using forward slashes.
   * @return A handle to the file; never {@code null}.
   */
  @NotNull
  default FlixelFile local(@NotNull String path) {
    return FlixelNoopFile.INSTANCE;
  }

  /**
   * Opens a handle to a file named by its full path on the underlying file system.
   *
   * @param path Absolute file system path.
   * @return A handle to the file; never {@code null}.
   */
  @NotNull
  default FlixelFile absolute(@NotNull String path) {
    return FlixelNoopFile.INSTANCE;
  }
}
