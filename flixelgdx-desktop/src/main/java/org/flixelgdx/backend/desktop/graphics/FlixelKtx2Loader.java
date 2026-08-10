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
package org.flixelgdx.backend.desktop.graphics;

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
 * Loads {@code .ktx2} compressed-texture files into GPU textures on the desktop backend.
 *
 * <p>KTX2 is a GPU container: it already holds pixels in a compressed format (such as BC7 or ASTC)
 * together with the mip chain. So, unlike a PNG, there is nothing to decode into RGBA on the CPU;
 * the whole file is handed to the GPU, which keeps it compressed. That saves both memory and upload
 * time versus an uncompressed texture.
 *
 * <p>The read happens off the main thread ({@link #loadRaw}); the GPU upload happens on the main
 * thread ({@link #finishRaw}) through {@link org.flixelgdx.graphics.FlixelGraphicsManager#createCompressedTexture
 * createCompressedTexture}, which the bgfx backend implements with its own container parser. Once a
 * {@code .ktx2} loader is registered, the asset manager transparently prefers a {@code .ktx2}
 * sibling over the plain image when one exists and compressed textures are enabled.
 */
public final class FlixelKtx2Loader implements FlixelAssetLoader<FlixelGraphic> {

  @NotNull
  @Override
  public Object loadRaw(@NotNull FlixelAssetManager assets, @NotNull String path, @NotNull FlixelFile file)
      throws Exception {
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
