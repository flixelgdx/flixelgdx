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

import org.flixelgdx.Flixel;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.graphics.FlixelNoopTexture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Safe default {@link FlixelAssetManager} used before a backend installs a real one and on
 * headless sessions.
 *
 * <p>Nothing is cached and nothing is ever really loaded: image requests produce size-only
 * stand-in textures and everything else produces empty strings, so game and test code that
 * touches assets before startup never crashes. Handles created here are not shared between
 * calls, so reference counting is effectively inert.
 */
public enum FlixelNoopAssetManager implements FlixelAssetManager {

  /** Shared no-op instance. */
  INSTANCE;

  @Override
  public void load(@NotNull String path) {}

  @Override
  public void load(@NotNull String path, boolean persist) {}

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public <T> FlixelAsset<T> get(@NotNull String path) {
    String key = FlixelAssetPaths.normalizeAssetPath(path);
    if (looksLikeImage(key)) {
      return (FlixelAsset<T>) new FlixelGraphic(this, key, new FlixelNoopTexture(1, 1));
    }
    return (FlixelAsset<T>) new FlixelDefaultAsset<String>(this, key);
  }

  @Nullable
  @Override
  public FlixelAsset<?> peek(@NotNull String path) {
    return null;
  }

  @Override
  public <T> void registerLoader(@NotNull String extension, @NotNull FlixelAssetLoader<T> loader) {}

  @Override
  public void unregisterLoader(@NotNull String extension) {}

  @Override
  public void register(@NotNull FlixelAsset<?> asset) {}

  @NotNull
  @Override
  public String allocateSyntheticKey() {
    return "flixel://noop";
  }

  @Override
  public boolean update() {
    return true;
  }

  @Override
  public boolean update(int millis) {
    return true;
  }

  @Override
  public float getProgress() {
    return 1f;
  }

  @Override
  public boolean isLoaded(@NotNull String path) {
    return false;
  }

  @Override
  public void finishLoading() {}

  @Override
  public void finishLoadingAsset(@NotNull String path) {}

  @Override
  public void unload(@NotNull String path) {}

  @NotNull
  @Override
  public String getDiagnostics() {
    return "No asset manager installed.";
  }

  @Override
  public void clearNonPersist() {}

  @Override
  public void clear() {}

  @Override
  public void destroy() {}

  @Override
  public boolean getGlobalPersist() {
    return false;
  }

  @Override
  public void setGlobalPersist(boolean globalPersist) {}

  @NotNull
  @Override
  public FlixelAssetMode getAssetMode() {
    return FlixelAssetMode.STANDARD;
  }

  @Override
  public void setAssetMode(@NotNull FlixelAssetMode mode) {}

  @Nullable
  @Override
  public Object getRaw(@NotNull String path) {
    return null;
  }

  @NotNull
  @Override
  public Object loadRawSync(@NotNull String path) {
    if (looksLikeImage(path)) {
      return new FlixelNoopTexture(1, 1);
    }
    return "";
  }

  @NotNull
  @Override
  public FlixelFile resolveFile(@NotNull String path) {
    return Flixel.files.internal(path);
  }

  @Override
  public void setFileResolver(@Nullable FlixelAssetFileResolver resolver) {}

  @NotNull
  @Override
  public String resolveTexturePath(@NotNull String path) {
    return FlixelAssetPaths.normalizeAssetPath(path);
  }

  private static boolean looksLikeImage(@NotNull String path) {
    String lower = path.toLowerCase();
    return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
        || lower.endsWith(".bmp") || lower.endsWith(".tga") || lower.endsWith(".ktx2");
  }
}
