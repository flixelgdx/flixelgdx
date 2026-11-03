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
package org.flixelgdx.backend.gpu;

import org.flixelgdx.Flixel;
import org.flixelgdx.asset.FlixelAsset;
import org.flixelgdx.asset.FlixelAssetLoader;
import org.flixelgdx.asset.FlixelAssetManager;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.graphics.FlixelGraphic;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Loads {@code .ktx2} compressed textures through the asset manager.
 *
 * <p>Once this loader is registered for {@code ".ktx2"}, the asset manager quietly prefers a {@code .ktx2} file
 * next to any requested image: asking for {@code "images/player.png"} loads {@code "images/player.ktx2"} when it
 * exists. Game code keeps using {@code .png} paths and gets a texture that stays compressed on the GPU. The FlixelGDX
 * Basis Universal Gradle plugin produces those files at build time.
 *
 * <p>The slow work (reading the file and transcoding Basis Universal data, see {@link FlixelKtx2}) happens in
 * {@link #loadRaw} on an asset loading thread. Only the upload happens on the render thread, in {@link #finishRaw}.
 *
 * <pre>{@code
 * Flixel.assets.registerLoader(".ktx2", new FlixelGpuKtx2Loader());
 * }</pre>
 *
 * <p>The Android backend registers it at launch. It needs the shared WebGPU renderer ({@link FlixelGpuGraphics}) to
 * be the installed graphics manager.
 */
public class FlixelGpuKtx2Loader implements FlixelAssetLoader<FlixelGraphic> {

  @NotNull
  @Override
  public Object loadRaw(@NotNull FlixelAssetManager assets, @NotNull String path, @NotNull FlixelFile file) {
    byte[] bytes = file.readBytes();
    if (bytes.length == 0) {
      throw new IllegalStateException("Compressed texture file not found or empty: '" + path + "'.");
    }
    ByteBuffer container = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
    container.put(bytes).flip();
    try {
      return renderer(path).prepareCompressedTexture(container);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Could not load compressed texture '" + path + "': " + e.getMessage(), e);
    }
  }

  @NotNull
  @Override
  public Object finishRaw(@NotNull FlixelAssetManager assets, @NotNull String path, @NotNull Object raw) {
    if (raw instanceof FlixelGpuCompressedImage image) {
      return renderer(path).createTexture(image);
    }
    return raw;
  }

  @NotNull
  @Override
  public FlixelAsset<FlixelGraphic> createHandle(@NotNull FlixelAssetManager assets, @NotNull String path) {
    return new FlixelGraphic(assets, path);
  }

  private static FlixelGpuGraphics renderer(String path) {
    if (Flixel.graphics instanceof FlixelGpuGraphics graphics) {
      return graphics;
    }
    throw new IllegalStateException("Compressed texture '" + path + "' needs the WebGPU renderer, which is not the"
        + " installed graphics manager.");
  }
}
