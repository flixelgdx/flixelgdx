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
package org.flixelgdx.graphics;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import org.flixelgdx.asset.FlixelAssetManager;
import org.flixelgdx.asset.FlixelWrapperFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

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
      if (g == null)
        continue;
      if (g.getRefCount() > 0)
        continue;

      if (g.isOwned()) {
        Texture t = g.getOwnedTexture();
        if (t != null) {
          t.dispose();
        }
      } else {
        if (g.isPersist())
          continue;
        if (am != null && am.isLoaded(g.getAssetKey(), Texture.class)) {
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
  public void forEachWrappedAsset(Consumer<FlixelGraphic> consumer) {
    for (FlixelGraphic graphic : cache.values()) {
      consumer.accept(graphic);
    }
  }

  @Override
  public void clearAll(@NotNull FlixelAssetManager assets) {
    AssetManager am = assets.getManager();
    for (ObjectMap.Entry<String, FlixelGraphic> e : cache) {
      FlixelGraphic g = e.value;
      if (g == null) {
        continue;
      }
      if (g.isOwned()) {
        Texture t = g.getOwnedTexture();
        if (t != null) {
          t.dispose();
        }
      } else {
        if (am != null && am.isLoaded(g.getAssetKey(), Texture.class)) {
          am.unload(g.getAssetKey());
        }
      }
    }
    cache.clear();
  }
}
