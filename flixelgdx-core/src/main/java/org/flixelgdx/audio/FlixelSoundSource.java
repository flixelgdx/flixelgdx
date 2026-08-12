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

import org.flixelgdx.Flixel;
import org.flixelgdx.asset.FlixelAsset;
import org.flixelgdx.asset.FlixelAssetManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Cached sound "source" (asset) that can spawn fresh {@link FlixelSound} instances on demand,
 * implementing {@link FlixelAsset}{@code <FlixelSoundSource>}.
 *
 * <p>Do not cache {@link FlixelSound} playback objects directly: a playback
 * object has mutable state (volume/pan/time/playing) and cannot be safely
 * shared across callers or overlapping plays. Cache this source instead and call
 * {@link #create()} for each play.
 *
 * <p>The source's content is a {@link FlixelSoundBuffer} (the encoded file bytes) held in the
 * asset manager's raw cache, so spawning a sound never touches the file system after loading.
 */
public final class FlixelSoundSource implements FlixelAsset<FlixelSoundSource> {

  @NotNull
  private final FlixelAssetManager assets;

  @NotNull
  private final String path;

  private int refCount;

  private boolean persist;

  /**
   * Creates a sound source for the given asset path.
   *
   * @param assets The owning asset manager.
   * @param path Normalized asset path.
   */
  public FlixelSoundSource(@NotNull FlixelAssetManager assets, @NotNull String path) {
    this.assets = Objects.requireNonNull(assets, "assets cannot be null.");
    this.path = Objects.requireNonNull(path, "path cannot be null.");
    this.persist = assets.getGlobalPersist();
  }

  @NotNull
  @Override
  public String getPath() {
    return path;
  }

  /** Returns {@code this}, since the source is its own handle. */
  @NotNull
  @Override
  public FlixelSoundSource get() {
    return this;
  }

  @Override
  public boolean isLoaded() {
    return assets.getRaw(path) != null;
  }

  /** Returns whether this source's audio bytes have been loaded into memory. */
  public boolean getLoaded() {
    return isLoaded();
  }

  @Override
  public boolean isPersist() {
    return persist;
  }

  /** Returns whether this source persists across state transitions. */
  public boolean getPersist() {
    return persist;
  }

  @NotNull
  @Override
  public FlixelSoundSource setPersist(boolean persist) {
    this.persist = persist;
    return this;
  }

  @Override
  public int getRefCount() {
    return refCount;
  }

  @NotNull
  @Override
  public FlixelSoundSource retain() {
    refCount++;
    return this;
  }

  @NotNull
  @Override
  public FlixelSoundSource release() {
    if (refCount <= 0) {
      refCount = 0;
      return this;
    }
    refCount--;
    if (refCount == 0) {
      assets.onAssetReleased(this);
    }
    return this;
  }

  /**
   * Returns this source's encoded audio bytes, block-loading them if they were never queued.
   *
   * @return The audio buffer; never {@code null}.
   */
  @NotNull
  public FlixelSoundBuffer getBuffer() {
    Object raw = assets.getRaw(path);
    if (raw == null) {
      raw = assets.loadRawSync(path);
    }
    if (!(raw instanceof FlixelSoundBuffer buffer)) {
      throw new IllegalStateException(
          "Asset at '" + path + "' is not audio (got " + raw.getClass().getSimpleName() + ").");
    }
    return buffer;
  }

  /**
   * Creates a new playable {@link FlixelSound} instance in the default SFX group.
   *
   * @return A new sound instance.
   */
  @NotNull
  public FlixelSound create() {
    return create(null);
  }

  /**
   * Creates a new playable {@link FlixelSound} instance in the provided group (or the default
   * SFX group if {@code null}).
   *
   * @param group The group for the new sound, or {@code null} for the SFX group.
   * @return A new sound instance.
   */
  @NotNull
  public FlixelSound create(@Nullable FlixelSoundGroup group) {
    FlixelSoundGroup targetGroup = (group != null) ? group : Flixel.sound.getSfxGroup();
    return Flixel.sound.getFactory().createSound(getBuffer(), targetGroup);
  }
}
