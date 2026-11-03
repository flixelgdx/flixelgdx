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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.stringdotjar.jwebgpu.GpuFeatureName;
import io.github.stringdotjar.jwebgpu.GpuTextureFormat;

/**
 * A way of storing texture pixels on the GPU: plain RGBA8, or one of the block-compressed formats.
 *
 * <p>A block-compressed format packs every 4x4 square of pixels into a fixed 8 or 16 bytes, which the GPU decodes
 * while sampling. A 16-byte format uses a quarter of the memory of RGBA8, and an 8-byte one an eighth, so textures
 * load faster and leave more room for everything else. Think of it like shipping flat-pack furniture: the box is
 * much smaller, and the GPU assembles each piece only when it needs it.
 *
 * <p>Each GPU supports only some of these formats. Mobile GPUs usually support {@link #ASTC_4X4} and the ETC2
 * family, while desktop GPUs support the BC family. {@link FlixelGpuGraphics#isBlockFormatSupported} tells which
 * ones the current device can use, and {@link #RGBA8} always works.
 *
 * <p>Every compressed format here maps to a {@code UNORM} GPU format, never an {@code SRGB} one. The renderer draws
 * PNG pixels as plain values without any color space conversion, so compressed textures must be sampled the same
 * way to look identical.
 */
public enum FlixelGpuBlockFormat {

  /** ASTC with 4x4 blocks: 16 bytes per block, with alpha. The best quality on mobile GPUs. */
  ASTC_4X4(GpuTextureFormat.ASTC_4X4_UNORM, GpuFeatureName.TEXTURE_COMPRESSION_ASTC, 4, 16, 157, 158),

  /** ETC2 with EAC alpha: 16 bytes per block. Supported by practically every Android GPU. */
  ETC2_RGBA8(GpuTextureFormat.ETC2_RGBA8_UNORM, GpuFeatureName.TEXTURE_COMPRESSION_ETC2, 4, 16, 151, 152),

  /** ETC2 without alpha: 8 bytes per block, half the size of {@link #ETC2_RGBA8}, for fully opaque textures. */
  ETC2_RGB8(GpuTextureFormat.ETC2_RGB8_UNORM, GpuFeatureName.TEXTURE_COMPRESSION_ETC2, 4, 8, 147, 148),

  /** BC7: 16 bytes per block, with alpha. The best quality on desktop GPUs. */
  BC7(GpuTextureFormat.BC7_RGBA_UNORM, GpuFeatureName.TEXTURE_COMPRESSION_BC, 4, 16, 145, 146),

  /** BC3 (also known as DXT5): 16 bytes per block, with alpha. */
  BC3(GpuTextureFormat.BC3_RGBA_UNORM, GpuFeatureName.TEXTURE_COMPRESSION_BC, 4, 16, 137, 138),

  /** BC1 (also known as DXT1): 8 bytes per block, with one bit of alpha. */
  BC1(GpuTextureFormat.BC1_RGBA_UNORM, GpuFeatureName.TEXTURE_COMPRESSION_BC, 4, 8, 133, 134),

  /** Uncompressed RGBA, 4 bytes per pixel. Every device supports it. */
  RGBA8(GpuTextureFormat.RGBA8_UNORM, null, 1, 4, 37, 43);

  private final GpuTextureFormat gpuFormat;
  private final GpuFeatureName feature;
  private final int blockSize;
  private final int blockBytes;
  private final int vkUnorm;
  private final int vkSrgb;

  FlixelGpuBlockFormat(@NotNull GpuTextureFormat gpuFormat, @Nullable GpuFeatureName feature, int blockSize,
      int blockBytes, int vkUnorm, int vkSrgb) {
    this.gpuFormat = gpuFormat;
    this.feature = feature;
    this.blockSize = blockSize;
    this.blockBytes = blockBytes;
    this.vkUnorm = vkUnorm;
    this.vkSrgb = vkSrgb;
  }

  /**
   * Finds the format that a KTX2 file's {@code vkFormat} field names.
   *
   * <p>KTX2 files name their pixel format with Vulkan's numbering. Both the {@code UNORM} and the {@code SRGB}
   * variant of a format map to the same constant, for the reason given in the class description.
   *
   * @param vkFormat The Vulkan format number.
   * @return The matching format, or {@code null} when the format is not one of these.
   */
  @Nullable
  public static FlixelGpuBlockFormat fromVkFormat(int vkFormat) {
    FlixelGpuBlockFormat[] all = values();
    for (int i = 0; i < all.length; i++) {
      if (all[i].vkUnorm == vkFormat || all[i].vkSrgb == vkFormat) {
        return all[i];
      }
    }
    return null;
  }

  /**
   * Returns how many blocks cover a row of pixels.
   *
   * @param pixels The width (or height) in pixels.
   * @return The number of blocks, rounded up so partial blocks count.
   */
  public int blocks(int pixels) {
    return (Math.max(1, pixels) + blockSize - 1) / blockSize;
  }

  /**
   * Rounds a size in pixels up to whole blocks.
   *
   * <p>The GPU requires compressed textures to be a whole number of blocks wide and high, so a 50x37 image becomes a
   * 52x40 texture. The extra pixels are never drawn.
   *
   * @param pixels The width (or height) in pixels.
   * @return The padded size in pixels.
   */
  public int padded(int pixels) {
    return blocks(pixels) * blockSize;
  }

  /**
   * Returns the number of bytes one mip level of the given size takes in this format.
   *
   * @param width The level width in pixels.
   * @param height The level height in pixels.
   * @return The size in bytes.
   */
  public int levelBytes(int width, int height) {
    return blocks(width) * blocks(height) * blockBytes;
  }

  /**
   * Returns whether this format packs pixels into blocks.
   *
   * @return {@code false} only for {@link #RGBA8}.
   */
  public boolean isCompressed() {
    return blockSize > 1;
  }

  /**
   * Returns this format's bit in a mask of supported formats.
   *
   * @return {@code 1 << ordinal()}.
   */
  public int bit() {
    return 1 << ordinal();
  }

  /**
   * Returns the WebGPU format a texture of this kind is created with.
   *
   * @return The texture format.
   */
  @NotNull
  public GpuTextureFormat getGpuFormat() {
    return gpuFormat;
  }

  /**
   * Returns the optional WebGPU feature a device needs to use this format.
   *
   * @return The feature, or {@code null} when every device supports the format.
   */
  @Nullable
  public GpuFeatureName getFeature() {
    return feature;
  }

  /**
   * Returns the width and height of one block, in pixels.
   *
   * @return 4 for compressed formats, 1 for {@link #RGBA8}.
   */
  public int getBlockSize() {
    return blockSize;
  }

  /**
   * Returns the number of bytes in one block.
   *
   * @return 8 or 16 for compressed formats, 4 for {@link #RGBA8}.
   */
  public int getBlockBytes() {
    return blockBytes;
  }
}
