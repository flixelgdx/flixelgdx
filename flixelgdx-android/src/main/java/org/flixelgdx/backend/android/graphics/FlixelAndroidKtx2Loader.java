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
package org.flixelgdx.backend.android.graphics;

import org.flixelgdx.Flixel;
import org.flixelgdx.asset.FlixelAsset;
import org.flixelgdx.asset.FlixelAssetLoader;
import org.flixelgdx.asset.FlixelAssetManager;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.graphics.FlixelTexture;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Loads {@code .ktx2} Basis Universal compressed-texture files on the Android backend.
 *
 * <p>KTX2 is a GPU container that stores pixels in a Basis-compressed format together with the
 * full mip chain. Rather than decoding to RGBA on the CPU (as a PNG would), this loader hands the
 * container to {@link org.flixelgdx.graphics.FlixelGraphicsManager#createCompressedTexture
 * createCompressedTexture}, which transcodes each mip level to the best GPU format this device
 * supports (ASTC 4x4, ETC2 RGBA8, or RGBA32 as a fallback) and uploads it in compressed form.
 * Keeping the texture compressed saves both GPU memory and upload bandwidth.
 *
 * <p>Register this loader once in the launcher and the asset manager will transparently prefer a
 * {@code .ktx2} sibling over the plain image whenever compressed textures are enabled.
 *
 * <p>The file read happens off the main thread in {@link #loadRaw}; the GPU upload happens on the
 * main thread in {@link #finishRaw}.
 */
public class FlixelAndroidKtx2Loader implements FlixelAssetLoader<FlixelGraphic> {

  @NotNull
  @Override
  public Object loadRaw(@NotNull FlixelAssetManager assets, @NotNull String path, @NotNull FlixelFile file) {
    byte[] bytes = file.readBytes();
    if (bytes.length == 0) {
      throw new IllegalStateException("Compressed texture file not found or empty: '" + path + "'.");
    }
    ByteBuffer container = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
    container.put(bytes).flip();
    return container;
  }

  @NotNull
  @Override
  public Object finishRaw(@NotNull FlixelAssetManager assets, @NotNull String path, @NotNull Object raw) {
    if (raw instanceof ByteBuffer container) {
      FlixelTexture texture = Flixel.graphics.createCompressedTexture(container);
      if (texture == null) {
        throw new IllegalStateException("Could not upload compressed texture: '" + path + "'.");
      }
      return texture;
    }
    return raw;
  }

  @NotNull
  @Override
  public FlixelAsset<FlixelGraphic> createHandle(@NotNull FlixelAssetManager assets, @NotNull String path) {
    return new FlixelGraphic(assets, path);
  }
}
