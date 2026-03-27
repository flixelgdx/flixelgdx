/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.graphics;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import me.stringdotjar.flixelgdx.asset.FlixelAssetManager;
import me.stringdotjar.flixelgdx.asset.FlixelWrapperFactory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Pooled {@link FlixelGraphic} factory for {@link FlixelAssetManager#obtainWrapper(String, Class)}.
 */
public final class FlixelGraphicWrapperFactory implements FlixelWrapperFactory<FlixelGraphic> {

  private final ObjectMap<String, FlixelGraphic> cache = new ObjectMap<>();

  @Override
  public Class<FlixelGraphic> wrapperType() {
    return FlixelGraphic.class;
  }

  @NotNull
  @Override
  public FlixelGraphic obtainKeyed(@NotNull FlixelAssetManager assets, @NotNull String key) {
    FlixelGraphic g = cache.get(key);
    if (g == null) {
      g = new FlixelGraphic(assets, key);
      cache.put(key, g);
    }
    return g;
  }

  @Nullable
  @Override
  public FlixelGraphic peek(@NotNull FlixelAssetManager assets, @NotNull String key) {
    return cache.get(key);
  }

  @Override
  public void registerInstance(@NotNull FlixelAssetManager assets, @NotNull FlixelGraphic wrapper) {
    cache.put(wrapper.getAssetKey(), wrapper);
  }

  @Override
  public void clearNonPersist(@NotNull FlixelAssetManager assets) {
    AssetManager am = assets.getManager();

    Array<String> toRemove = null;
    for (ObjectMap.Entry<String, FlixelGraphic> e : cache) {
      FlixelGraphic g = e.value;
      if (g == null) continue;
      if (g.isPersist()) continue;
      if (g.getRefCount() > 0) continue;

      if (g.isOwned()) {
        Texture t = g.getOwnedTexture();
        if (t != null) {
          t.dispose();
        }
      } else if (am != null) {
        if (am.isLoaded(g.getAssetKey(), Texture.class)) {
          am.unload(g.getAssetKey());
        }
      }

      if (toRemove == null) {
        toRemove = new Array<>();
      }
      toRemove.add(g.getAssetKey());
    }

    if (toRemove != null) {
      for (int i = 0; i < toRemove.size; i++) {
        cache.remove(toRemove.get(i));
      }
    }
  }

  @Override
  public void clearAll() {
    cache.clear();
  }
}
