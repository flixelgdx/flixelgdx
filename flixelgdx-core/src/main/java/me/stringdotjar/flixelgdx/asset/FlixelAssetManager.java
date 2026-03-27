/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.asset;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;

import me.stringdotjar.flixelgdx.FlixelDestroyable;
import me.stringdotjar.flixelgdx.graphics.FlixelGraphic;
import me.stringdotjar.flixelgdx.graphics.FlixelGraphicSource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Asset manager interface for FlixelGDX.
 *
 * <p>This is the public seam used by sprites and other runtime systems. The default implementation is
 * {@link FlixelDefaultAssetManager}.
 */
public interface FlixelAssetManager extends FlixelDestroyable, Disposable {

  <T> void load(@NotNull String fileName, @NotNull Class<T> type);

  void load(@NotNull FlixelSource<?> source);

  void load(@NotNull AssetDescriptor<?> assetDescriptor);

  boolean update();

  boolean update(int millis);

  float getProgress();

  boolean isLoaded(@NotNull String fileName);

  boolean isLoaded(@NotNull String fileName, @NotNull Class<?> type);

  boolean isLoaded(@NotNull FlixelSource<?> source);

  @NotNull
  <T> T get(@NotNull String fileName, @NotNull Class<T> type);

  @NotNull
  <T> T get(@NotNull FlixelSource<T> source);

  @NotNull
  <T> T require(@NotNull FlixelSource<T> source);

  @NotNull
  Texture requireTexture(@NotNull String assetKey);

  @NotNull
  default Texture requireTexture(@NotNull FlixelGraphicSource source) {
    return requireTexture(source.getAssetKey());
  }

  @NotNull
  Texture loadGraphicSourceNow(@NotNull String assetKey);

  @NotNull
  String resolveAudioPath(@NotNull String path);

  @NotNull
  String extractAssetPath(@NotNull String path);

  void unload(@NotNull String fileName);

  void finishLoading();

  void finishLoadingAsset(@NotNull String fileName);

  @NotNull
  String getDiagnostics();

  void clearNonPersist();

  @NotNull
  FlixelGraphic obtainGraphic(@NotNull String assetKey);

  @NotNull
  FlixelGraphic obtainOwnedGraphic(@NotNull Texture texture);

  @Nullable
  FlixelGraphic peekGraphic(@NotNull String assetKey);

  void clearNonPersistGraphics();

  @NotNull
  <T> FlixelAsset<T> obtainTypedAsset(@NotNull String assetKey, @NotNull Class<T> type);

  @Nullable
  FlixelAsset<?> peekTypedAsset(@NotNull String assetKey, @NotNull Class<?> type);

  void clearNonPersistTypedAssets();

  @NotNull
  AssetManager getManager();
}
