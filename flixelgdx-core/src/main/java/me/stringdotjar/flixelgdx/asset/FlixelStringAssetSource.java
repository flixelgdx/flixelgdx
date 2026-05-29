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
package me.stringdotjar.flixelgdx.asset;

import org.jetbrains.annotations.NotNull;

/**
 * Source for plain text assets loaded via the libGDX {@link String} asset loader.
 *
 * <p>Used by extension-based {@link FlixelAssetManager#load(String)} for paths like {@code .txt},
 * {@code .xml}, {@code .json} when registered on the manager.
 */
public final class FlixelStringAssetSource implements FlixelSource<String> {

  @NotNull
  private final String assetKey;

  public FlixelStringAssetSource(@NotNull String assetKey) {
    if (assetKey == null || assetKey.isEmpty()) {
      throw new IllegalArgumentException("assetKey cannot be null/empty.");
    }
    this.assetKey = FlixelAssetPaths.normalizeAssetPath(assetKey);
  }

  @Override
  public String getAssetKey() {
    return assetKey;
  }

  @Override
  public Class<String> getType() {
    return String.class;
  }
}
