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
package org.flixelgdx.text;

import org.flixelgdx.Flixel;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.graphics.FlixelImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Global registry of fonts available to {@link FlixelText}.
 *
 * <p>Register a scalable font file ({@code .ttf} or {@code .otf}) once under a short id, then
 * reference it from any number of text objects. Baked {@link FlixelFont} instances are cached
 * per id, size, and filter so repeated use never re-rasterizes:
 *
 * <pre>{@code
 * // At startup:
 * FlixelFontRegistry.register("main", Flixel.files.internal("fonts/Nunito.ttf"));
 * FlixelFontRegistry.setDefault("main");
 *
 * // Anywhere:
 * FlixelText title = new FlixelText(20, 20, 0, "Hello!", 32);
 * title.setFont("main");
 * }</pre>
 *
 * <p>Scalable fonts bake through the platform's {@link FlixelFontRasterizer}, installed by the
 * backend via {@link #setRasterizer(FlixelFontRasterizer)}. When no rasterizer or no
 * registered font is available, text falls back to the packaged bitmap default font
 * (lsans-15, an AngelCode {@code .fnt}), scaled to the requested size, so text always renders.
 */
public final class FlixelFontRegistry {

  private static final String PACKAGED_FONT_BASE = "org/flixelgdx/bitmap/lsans-15";

  /** Registered font file bytes by id. */
  private static final FlixelMap<String, byte[]> fontData = new FlixelMap<>();

  /** Baked fonts keyed by {@code id|size|smooth}. */
  private static final FlixelMap<String, FlixelFont> bakedFonts = new FlixelMap<>();

  @Nullable
  private static FlixelFontRasterizer rasterizer;

  @Nullable
  private static FlixelFont packagedDefault;

  @Nullable
  private static String defaultFontId;

  private static boolean packagedDefaultFailed;

  private FlixelFontRegistry() {}

  /**
   * Installs the platform's font rasterizer. Called by the backend launcher before startup;
   * replacing it invalidates nothing that is already baked.
   *
   * @param newRasterizer The rasterizer, or {@code null} to leave scalable fonts unavailable.
   */
  public static void setRasterizer(@Nullable FlixelFontRasterizer newRasterizer) {
    rasterizer = newRasterizer;
  }

  /**
   * Registers a scalable font file under an id, replacing any previous registration and
   * destroying fonts baked from it.
   *
   * @param id A short identifier such as {@code "main"}.
   * @param fontFile The {@code .ttf} or {@code .otf} file to read.
   */
  public static void register(@NotNull String id, @NotNull FlixelFile fontFile) {
    byte[] data = fontFile.readBytes();
    if (data.length == 0) {
      Flixel.warn("Fonts", "Font file for id '" + id + "' is missing or empty: " + fontFile.getPath());
      return;
    }
    if (fontData.containsKey(id)) {
      removeBakedFontsForId(id);
    }
    fontData.put(id, data);
  }

  /**
   * Removes a registered font and destroys everything baked from it.
   *
   * @param id The id passed to {@link #register}.
   */
  public static void unregister(String id) {
    if (fontData.remove(id) != null) {
      removeBakedFontsForId(id);
    }
    if (id != null && id.equals(defaultFontId)) {
      defaultFontId = null;
    }
  }

  /**
   * Returns whether a font is registered under {@code id}.
   *
   * @param id The id to check.
   * @return {@code true} when registered.
   */
  public static boolean has(String id) {
    return id != null && fontData.containsKey(id);
  }

  /**
   * Returns a snapshot of all registered ids.
   *
   * @return A freshly built list of ids; never {@code null}.
   */
  @NotNull
  public static FlixelArray<String> getRegisteredIds() {
    FlixelArray<String> ids = new FlixelArray<>(fontData.getSize());
    for (String key : fontData.keys()) {
      ids.add(key);
    }
    return ids;
  }

  /**
   * Marks a registered font as the default used by {@link FlixelText} objects that never call
   * {@code setFont}.
   *
   * @param id The id passed to {@link #register}, or {@code null} to fall back to the packaged font.
   */
  public static void setDefault(@Nullable String id) {
    defaultFontId = id;
  }

  /**
   * @return The current default font id, or {@code null} when the packaged font is the default.
   */
  @Nullable
  public static String getDefault() {
    return defaultFontId;
  }

  /**
   * Returns a baked font for a registered id at the given pixel size, baking and caching it on
   * first use.
   *
   * @param id The id passed to {@link #register}.
   * @param pixelSize The bake size in pixels per line.
   * @param smooth {@code true} for linear filtering on the atlas.
   * @return The baked font, or {@code null} when the id is unknown or no rasterizer is installed.
   */
  @Nullable
  public static FlixelFont getFont(@NotNull String id, int pixelSize, boolean smooth) {
    String key = id + "|" + pixelSize + "|" + smooth;
    FlixelFont cached = bakedFonts.get(key);
    if (cached != null) {
      return cached;
    }
    byte[] data = fontData.get(id);
    FlixelFontRasterizer raster = rasterizer;
    if (data == null || raster == null) {
      return null;
    }
    FlixelRasterizedFont opened = raster.open(data, pixelSize);
    if (opened == null) {
      Flixel.warn("Fonts", "Could not open font '" + id + "'; is it a valid .ttf/.otf file?");
      return null;
    }
    FlixelFont baked = FlixelFont.bake(opened, smooth);
    opened.destroy();
    bakedFonts.put(key, baked);
    return baked;
  }

  /**
   * Bakes a font directly from a file, cached under the file's path.
   *
   * @param fontFile The {@code .ttf} or {@code .otf} file to read.
   * @param pixelSize The bake size in pixels per line.
   * @param smooth {@code true} for linear filtering on the atlas.
   * @return The baked font, or {@code null} when the file is unreadable or no rasterizer exists.
   */
  @Nullable
  public static FlixelFont getFont(@NotNull FlixelFile fontFile, int pixelSize, boolean smooth) {
    String id = "file:" + fontFile.getPath();
    if (!fontData.containsKey(id)) {
      byte[] data = fontFile.readBytes();
      if (data.length == 0) {
        return null;
      }
      fontData.put(id, data);
    }
    return getFont(id, pixelSize, smooth);
  }

  /**
   * Returns the font the default cascade resolves to: the {@link #setDefault default}
   * registered font when one is set and bakeable, otherwise the packaged bitmap font.
   *
   * @param pixelSize The desired size in pixels (used when baking a registered default).
   * @param smooth {@code true} for linear filtering.
   * @return The default font, or {@code null} when even the packaged font cannot be loaded.
   */
  @Nullable
  public static FlixelFont getDefaultFont(int pixelSize, boolean smooth) {
    if (defaultFontId != null) {
      FlixelFont font = getFont(defaultFontId, pixelSize, smooth);
      if (font != null) {
        return font;
      }
    }
    return getPackagedFont();
  }

  /**
   * Returns the packaged lsans-15 bitmap font, loading and caching it on first use.
   *
   * @return The packaged font, or {@code null} when its resources cannot be read.
   */
  @Nullable
  public static FlixelFont getPackagedFont() {
    if (packagedDefault != null) {
      return packagedDefault;
    }
    if (packagedDefaultFailed) {
      return null;
    }
    byte[] fnt = readPackagedResource(PACKAGED_FONT_BASE + ".fnt");
    if (fnt.length == 0) {
      packagedDefaultFailed = true;
      Flixel.warn("Fonts", "Packaged default font is unavailable; text will not render.");
      return null;
    }
    byte[] png = readPackagedResource(PACKAGED_FONT_BASE + ".png");
    FlixelImage pageImage = null;
    if (png.length > 0) {
      ByteBuffer encoded = ByteBuffer.allocateDirect(png.length).order(ByteOrder.nativeOrder());
      encoded.put(png).flip();
      pageImage = Flixel.graphics.decodeImage(encoded);
    }
    packagedDefault = FlixelFont.fromFnt(new String(fnt, StandardCharsets.UTF_8), pageImage);
    return packagedDefault;
  }

  /** Destroys every baked font and clears all registrations. Called at game shutdown. */
  public static void dispose() {
    for (FlixelMap.Entry<String, FlixelFont> entry : bakedFonts.entries()) {
      entry.value.destroy();
    }
    bakedFonts.clear();
    fontData.clear();
    if (packagedDefault != null) {
      packagedDefault.destroy();
      packagedDefault = null;
    }
    packagedDefaultFailed = false;
    defaultFontId = null;
  }

  /** Destroys and forgets every baked font derived from one registered id. */
  private static void removeBakedFontsForId(@NotNull String id) {
    String prefix = id + "|";
    FlixelMap.Entries<String, FlixelFont> it = bakedFonts.entries();
    while (it.hasNext()) {
      FlixelMap.Entry<String, FlixelFont> entry = it.next();
      if (entry.key.startsWith(prefix)) {
        entry.value.destroy();
        it.remove();
      }
    }
  }

  /**
   * Reads a packaged resource, first through the file seam's classpath root and then through
   * the class loader, so the font loads both in development and from inside a JAR.
   */
  private static byte @NotNull [] readPackagedResource(@NotNull String path) {
    FlixelFile viaSeam = Flixel.files.classpath(path);
    if (viaSeam.exists()) {
      byte[] bytes = viaSeam.readBytes();
      if (bytes.length > 0) {
        return bytes;
      }
    }
    try (InputStream in = FlixelFontRegistry.class.getResourceAsStream("/" + path)) {
      if (in == null) {
        return new byte[0];
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream(16384);
      byte[] chunk = new byte[8192];
      int read;
      while ((read = in.read(chunk)) > 0) {
        out.write(chunk, 0, read);
      }
      return out.toByteArray();
    } catch (Exception e) {
      return new byte[0];
    }
  }
}
