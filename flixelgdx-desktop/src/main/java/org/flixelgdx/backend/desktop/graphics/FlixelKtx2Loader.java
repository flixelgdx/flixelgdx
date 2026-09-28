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
import org.flixelgdx.backend.desktop.FlixelDesktopLauncher;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.graphics.FlixelTexture;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Loads GPU-native {@code .ktx2} compressed-texture files into GPU textures on the desktop backend.
 *
 * <p>KTX2 is a GPU container: it can hold pixels in a compressed format the GPU reads directly
 * (such as BC7) together with the mip chain. So, unlike a PNG, there is nothing to decode into RGBA
 * on the CPU; the whole file is handed to the GPU, which keeps it compressed. That saves both memory
 * and upload time versus an uncompressed texture.
 *
 * <p>This loader only works with KTX2 files that bgfx can parse on its own: a concrete GPU format
 * and no supercompression. It does <b>not</b> accept the Basis Universal files written by the
 * {@code basisu} Gradle plugin (ETC1S or UASTC), since those store an undefined pixel format that
 * must be transcoded first, and the desktop backend has no transcoder. For that reason
 * {@link FlixelDesktopLauncher} does not register this loader, and desktop builds load the plain
 * PNGs instead.
 *
 * <p>To opt in for your own GPU-native KTX2 files, register it before loading any textures:
 *
 * <pre>{@code
 * Flixel.assets.registerLoader(".ktx2", new FlixelKtx2Loader());
 * Flixel.assets.setCompressedTexturesEnabled(true);
 * }</pre>
 *
 * <p>The read happens off the main thread ({@link #loadRaw}); the GPU upload happens on the main
 * thread ({@link #finishRaw}) through {@link FlixelGraphicsManager#createCompressedTexture
 * createCompressedTexture}, which the bgfx backend implements with its own container parser. Once a
 * {@code .ktx2} loader is registered, the asset manager transparently prefers a {@code .ktx2}
 * sibling over the plain image when one exists and compressed textures are enabled.
 */
public class FlixelKtx2Loader implements FlixelAssetLoader<FlixelGraphic> {

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
        throw new IllegalStateException("Could not upload compressed texture: '" + path + "'. bgfx only"
            + " accepts KTX2 files with a GPU-native format and no supercompression; Basis Universal"
            + " (ETC1S or UASTC) files are not supported on desktop.");
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
