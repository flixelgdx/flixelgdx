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
package me.stringdotjar.flixelgdx.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import me.stringdotjar.flixelgdx.Flixel;

// TODO: Remove this class and find a better way to generically handle paths, this is only here because of
// FlixelGDX recently being moved from Polyverse.

/** Utility class for simplifying asset paths and libGDX {@link FileHandle}s. */
public final class FlixelPathsUtil {

  public static FileHandle asset(String path) {
    return Gdx.files.internal(path);
  }

  public static FileHandle shared(String path) {
    return asset(String.format("shared/%s", path));
  }

  public static FileHandle fontAsset(String path) {
    return asset(String.format("fonts/%s.ttf", path));
  }

  public static FileHandle xmlAsset(String path) {
    return asset(String.format("%s.xml", path));
  }

  public static FileHandle sharedImageAsset(String path) {
    return shared(String.format("images/%s.png", path));
  }

  public static FileHandle external(String path) {
    return Gdx.files.external(path);
  }

  /**
   * Resolves an internal asset path to an absolute filesystem path that the
   * native audio backend can open directly.
   *
   * <p>When running from the IDE the working directory is the {@code assets/}
   * folder, so the raw relative path works as-is. When running from a packaged
   * JAR the assets are embedded as classpath resources and native engines cannot
   * open them by name. In that case the resource is extracted to a temp file on
   * first call, and the temp file's absolute path is returned. Results are
   * cached so repeated calls for the same path do not produce extra temp files.
   *
   * @param path The internal asset path, e.g. {@code "shared/sounds/foo.ogg"}.
   * @return An absolute filesystem path that the audio backend can open.
   * @see me.stringdotjar.flixelgdx.asset.FlixelAssetManager#resolveAudioPath(String)
   */
  public static String resolveAudioPath(String path) {
    return Flixel.ensureAssets().resolveAudioPath(path);
  }

  private FlixelPathsUtil() {}
}
