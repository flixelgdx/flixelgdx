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
package org.flixelgdx.backend.html5.file;

import org.flixelgdx.backend.html5.file.FlixelHtml5File.Kind;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.file.FlixelFiles;
import org.jetbrains.annotations.NotNull;

/**
 * The web file system, mapping FlixelGDX's file roots onto what a browser actually provides.
 *
 * <p>A browser can only do two file-like things: download resources from its origin server and read
 * or write small string entries in {@code localStorage}. This backend routes the framework's roots
 * to whichever of those fits:
 *
 * <ul>
 *   <li>{@link #internal(String)} and {@link #classpath(String)} become downloads from the
 *       {@code assets/} folder the build plugin publishes next to the page.</li>
 *   <li>{@link #external(String)}, {@link #local(String)}, and {@link #pref(String, String, String)}
 *       become {@code localStorage} entries, which persist between sessions on the same origin and
 *       are the browser's closest equivalent to a save file.</li>
 *   <li>{@link #absolute(String)} is treated as a full URL download, since the browser has no
 *       absolute file system path.</li>
 * </ul>
 */
public class FlixelHtml5Files implements FlixelFiles {

  /** The URL prefix under which the build plugin publishes bundled assets. */
  public static final String ASSET_ROOT = "assets/";

  /** The URL of the asset manifest the preloader reads at startup. */
  public static final String ASSET_MANIFEST = "assets/assets.txt";

  /** The {@code localStorage} key prefix that namespaces all framework-managed storage entries. */
  private static final String STORAGE_PREFIX = "flixel:";

  @Override
  @NotNull
  public FlixelFile internal(@NotNull String path) {
    return new FlixelHtml5File(path, strip(path), Kind.ASSET);
  }

  @Override
  @NotNull
  public FlixelFile classpath(@NotNull String path) {
    return new FlixelHtml5File(path, strip(path), Kind.ASSET);
  }

  @Override
  @NotNull
  public FlixelFile external(@NotNull String path) {
    return new FlixelHtml5File(path, STORAGE_PREFIX + "external:" + path, Kind.STORAGE);
  }

  @Override
  @NotNull
  public FlixelFile local(@NotNull String path) {
    return new FlixelHtml5File(path, STORAGE_PREFIX + "local:" + path, Kind.STORAGE);
  }

  @Override
  @NotNull
  public FlixelFile absolute(@NotNull String path) {
    return new FlixelHtml5File(path, path, Kind.ASSET);
  }

  @Override
  @NotNull
  public FlixelFile pref(@NotNull String org, @NotNull String app, @NotNull String path) {
    return new FlixelHtml5File(path, STORAGE_PREFIX + org + ":" + app + ":" + path, Kind.STORAGE);
  }

  /**
   * Removes a single leading slash so asset paths join cleanly onto {@link #ASSET_ROOT}.
   *
   * @param path The requested path.
   * @return The path without a leading slash.
   */
  private static String strip(String path) {
    return path.startsWith("/") ? path.substring(1) : path;
  }
}
