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
package org.flixelgdx.asset;

import org.flixelgdx.file.FlixelFile;
import org.jetbrains.annotations.NotNull;

/**
 * Turns an asset path into the {@link FlixelFile} it should be read from.
 *
 * <p>The default resolver used by asset managers is {@code Flixel.files.internal(path)}, which
 * reads from the game's asset folder on disk. Install a custom resolver with
 * {@link FlixelAssetManager#setFileResolver(FlixelAssetFileResolver)} when your game also needs
 * to run packaged inside a JAR, where assets live on the classpath instead:
 *
 * <pre>{@code
 * Flixel.assets.setFileResolver(path -> {
 *   FlixelFile onDisk = Flixel.files.internal(path);
 *   return onDisk.exists() ? onDisk : Flixel.files.classpath(path);
 * });
 * }</pre>
 *
 * <p>Writing the helper once is slightly more work than passing raw strings around, but it is
 * safer: every asset read in the framework flows through the same decision, so a path that
 * works in development keeps working in the packaged build.
 */
@FunctionalInterface
public interface FlixelAssetFileResolver {

  /**
   * Resolves an asset path to a readable file.
   *
   * @param path Normalized asset path (e.g. {@code "images/player.png"}).
   * @return The file to read; never {@code null}, though it may not exist.
   */
  @NotNull
  FlixelFile resolve(@NotNull String path);
}
