/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

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
    this.assetKey = assetKey;
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
