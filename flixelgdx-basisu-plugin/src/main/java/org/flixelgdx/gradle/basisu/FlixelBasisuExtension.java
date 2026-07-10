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
package org.flixelgdx.gradle.basisu;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * Configuration extension exposed as the {@code flixelBasisu} DSL block by {@link FlixelBasisuPlugin}.
 *
 * <p>All properties have sensible defaults and are optional. Compression itself stays off by
 * default; enable it by passing {@code -PenableBasisuCompression=true} or setting {@code enabled}
 * directly.
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * flixelBasisu {
 *   // Explicit override; otherwise reads the enableBasisuCompression Gradle property (default false).
 *   enabled = true
 *
 *   // Directory scanned for .png files to compress (default: rootProject/assets/).
 *   assetsDir = file('../assets')
 *
 *   // Where compressed .ktx2 files are written (default: build/generated/basisuAssets).
 *   outputDir = file('build/compressedAssets')
 *
 *   // Generate mipmaps for each compressed texture (default: true).
 *   generateMipmaps = true
 *
 *   // Use higher-quality UASTC instead of the smaller default ETC1S mode (default: false).
 *   useUastc = false
 *
 *   // ETC1S quality level, 1 (smallest, worst) to 255 (largest, best). Ignored when useUastc is
 *   // true. Default: 128.
 *   etc1sQuality = 128
 *
 *   // UASTC encoding level, 0 (fastest, worst) to 4 (slowest, best). Ignored when useUastc is
 *   // false. Default: 2.
 *   uastcLevel = 2
 *
 *   // Ant-style glob patterns, relative to assetsDir, to skip. A plain path excludes just that
 *   // one file; a path ending in /** excludes an entire folder (e.g. bitmap fonts).
 *   excludes = [
 *     'foo/bar/icon.png',
 *     'fonts/**'
 *   ]
 * }
 * }</pre>
 *
 * @see FlixelBasisuPlugin
 */
public interface FlixelBasisuExtension {

  /** Gradle extension name used to register this extension under. */
  String NAME = "flixelgdxBasisu";

  /**
   * Whether texture compression runs at all.
   *
   * <p>Defaults to the {@code enableBasisuCompression} Gradle project property (itself defaulting
   * to {@code false}), so a generated project ships with compression available but inactive until
   * a developer opts in.
   *
   * @return The {@code enabled} property.
   */
  Property<Boolean> getEnabled();

  /**
   * Directory scanned (recursively) for {@code .png} files to compress.
   *
   * <p>Defaults to the {@code assets/} directory at the root of the Gradle project.
   *
   * @return The assets source directory property.
   */
  DirectoryProperty getAssetsDir();

  /**
   * Directory that compressed {@code .ktx2} files are written into, mirroring the relative path
   * of each source image under {@link #getAssetsDir()}.
   *
   * <p>Defaults to {@code build/generated/basisuAssets}. {@link FlixelBasisuPlugin} adds this
   * directory as an extra Android assets source set directory when applied to an Android module.
   *
   * @return The compressed output directory property.
   */
  DirectoryProperty getOutputDir();

  /**
   * Whether the encoder generates a full mipmap chain for each compressed texture.
   *
   * <p>Defaults to {@code true}.
   *
   * @return The {@code generate-mipmaps} property.
   */
  Property<Boolean> getGenerateMipmaps();

  /**
   * Whether to use the higher-quality UASTC encoding mode instead of the default ETC1S mode.
   *
   * <p>UASTC produces noticeably larger files in exchange for higher visual fidelity. Defaults to
   * {@code false} (ETC1S), which is adequate for most 2D sprite work.
   *
   * @return The {@code use-uastc} property.
   */
  Property<Boolean> getUseUastc();

  /**
   * ETC1S quality level, from {@code 1} (smallest file, worst quality) to {@code 255} (largest
   * file, best quality).
   *
   * <p>Only honored when {@link #getUseUastc()} is {@code false}; ignored in UASTC mode. Defaults
   * to {@code 128}, matching the {@code basisu} encoder's own default.
   *
   * @return The {@code -q} property.
   */
  Property<Integer> getEtc1sQuality();

  /**
   * UASTC encoding level, from {@code 0} (fastest, lowest quality) to {@code 4} (slowest, highest
   * quality).
   *
   * <p>Only honored when {@link #getUseUastc()} is {@code true}; ignored in ETC1S mode. Defaults
   * to {@code 2}, matching the {@code basisu} encoder's own default.
   *
   * @return The {@code -uastc_level} property.
   */
  Property<Integer> getUastcLevel();

  /**
   * Ant-style glob patterns, relative to {@link #getAssetsDir()}, for {@code .png} files that
   * should stay as plain PNGs instead of being compressed.
   *
   * <p>A plain path (for example, {@code "foo/bar/icon.png"}) excludes just that one file.
   * A path ending in {@code /**} (for example, {@code "fonts/**"}) excludes an entire folder and
   * everything under it. Defaults to an empty list. Useful for assets where exact pixel data
   * matters, such as bitmap font pages.
   *
   * @return The excludes property.
   */
  ListProperty<String> getExcludes();
}
