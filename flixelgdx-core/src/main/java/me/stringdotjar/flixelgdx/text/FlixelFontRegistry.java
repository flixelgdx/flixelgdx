/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.text;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;

import me.stringdotjar.flixelgdx.FlixelDestroyable;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A global registry for TrueType ({@code .ttf}/{@code .otf}) fonts that can be used
 * with {@link FlixelText}.
 *
 * <p>Fonts are registered once with a unique string identifier and an asset path, then
 * referenced by that identifier throughout the game. The registry caches
 * {@link FreeTypeFontGenerator} instances internally so that multiple {@link FlixelText}
 * objects sharing the same font ID reuse the same generator, avoiding redundant file parsing.
 * Generated {@link BitmapFont} instances are also cached (keyed by font source and glyph
 * parameters) so repeated {@link FlixelText} instances with the same settings share one
 * texture-backed font. The built-in libGDX default bitmap font is cached per pixel size.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Register fonts at startup (e.g. in your FlixelState.create()):
 * FlixelFontRegistry.register("pixel", Gdx.files.internal("fonts/pixel.ttf"));
 * FlixelFontRegistry.register("ui",    Gdx.files.internal("fonts/opensans.ttf"));
 *
 * // Optionally set a global default so FlixelText objects use it automatically:
 * FlixelFontRegistry.setDefault("ui");
 *
 * // Use in FlixelText:
 * FlixelText title = new FlixelText(0, 0, 0, "Hello!", 32);
 * title.setFont("pixel");
 *
 * // Clean up when the game shuts down:
 * FlixelFontRegistry.dispose();
 * }</pre>
 *
 * <h2>Lifecycle</h2>
 * <p>{@link #dispose()} is called when the game shuts down (in
 * {@code FlixelGame.dispose()}) to release all cached generators. Individual entries
 * can be removed earlier with {@link #unregister(String)}.
 */
public final class FlixelFontRegistry {

  private static final Map<String, Entry> entries = new HashMap<>();

  /** The ID of the font that FlixelText uses when no explicit font is set. */
  private static String defaultFontId;

  /**
   * Shared libGDX default {@link BitmapFont} instances, one per requested pixel size (after
   * scaling the built-in 15px font to match).
   */
  private static final IntMap<BitmapFont> defaultBitmapFontsBySize = new IntMap<>();

  /**
   * FreeType-generated bitmap fonts keyed by {@link #freeTypeBitmapFontKey(String, int, int)}.
   */
  private static final ObjectMap<String, BitmapFont> freeTypeBitmapFonts = new ObjectMap<>();

  private FlixelFontRegistry() {}

  /**
   * Registers a TrueType font under the given identifier. If an entry with the same
   * ID already exists, it is replaced (and its cached generator is disposed).
   *
   * @param id A unique identifier for this font (e.g. {@code "pixel"}, {@code "main"}, {@code "bold"}).
   * @param fontFile A libGDX {@link FileHandle} pointing to the {@code .ttf} or {@code .otf} asset.
   * @throws IllegalArgumentException if {@code id} is {@code null}/empty or {@code fontFile} is {@code null}.
   */
  public static void register(@NotNull String id, @NotNull FileHandle fontFile) {
    if (id.isEmpty()) {
      throw new IllegalArgumentException("Font ID must not be empty.");
    }

    Entry existing = entries.get(id);
    if (existing != null) {
      removeFreeTypeBitmapFontsForPrefix("reg:" + id + "|");
      existing.destroy();
    }
    entries.put(id, new Entry(fontFile));
  }

  /**
   * Removes a previously registered font and disposes its cached generator.
   * Does nothing if the ID is not registered. If the removed font was the
   * {@linkplain #setDefault(String) default}, the default is cleared.
   *
   * @param id The font identifier to remove.
   */
  public static void unregister(String id) {
    if (id != null) {
      removeFreeTypeBitmapFontsForPrefix("reg:" + id + "|");
      if (id.equals(defaultFontId)) {
        removeFreeTypeBitmapFontsForPrefix("def:" + id + "|");
        defaultFontId = null;
      }
    }
    Entry removed = entries.remove(id);
    if (removed != null) {
      removed.destroy();
    }
  }

  /**
   * Returns whether a font with the given ID is registered.
   *
   * @param id The font identifier to check.
   * @return {@code true} if the font is registered.
   */
  public static boolean has(String id) {
    return entries.containsKey(id);
  }

  /**
   * Returns the {@link FileHandle} for a registered font.
   *
   * @param id The font identifier.
   * @return The font's file handle.
   * @throws IllegalArgumentException if the ID is not registered.
   */
  public static FileHandle getFile(String id) {
    Entry entry = requireEntry(id);
    return entry.fontFile;
  }

  /**
   * Returns the cached {@link FreeTypeFontGenerator} for a registered font,
   * creating it lazily on first access. The generator is owned by the registry
   * and must <em>not</em> be disposed by the caller.
   *
   * @param id The font identifier.
   * @return The shared font generator.
   * @throws IllegalArgumentException if the ID is not registered.
   */
  public static FreeTypeFontGenerator getGenerator(String id) {
    Entry entry = requireEntry(id);
    return entry.getOrCreateGenerator();
  }

  /**
   * Returns an unmodifiable view of all currently registered font IDs.
   *
   * @return A set of registered font identifiers.
   */
  public static Set<String> getRegisteredIds() {
    return Collections.unmodifiableSet(entries.keySet());
  }

  /**
   * Sets the global default font that {@link FlixelText} will use when no explicit
   * font is set via {@link FlixelText#setFont(FileHandle)} or
   * {@link FlixelText#setFont(String)}. Pass {@code null} to clear the default,
   * which causes FlixelText to fall back to libGDX's built-in bitmap font.
   *
   * @param id The registered font ID to use as the default, or {@code null} to clear.
   * @throws IllegalArgumentException if {@code id} is non-null but not registered.
   */
  public static void setDefault(String id) {
    if (id != null && !entries.containsKey(id)) {
      throw new IllegalArgumentException("Font id \"" + id + "\" is not registered.");
    }
    if (defaultFontId != null && !defaultFontId.equals(id)) {
      removeFreeTypeBitmapFontsForPrefix("def:" + defaultFontId + "|");
    }
    defaultFontId = id;
  }

  /**
   * Returns the ID of the current default font, or {@code null} if none is set.
   */
  public static String getDefault() {
    return defaultFontId;
  }

  /**
   * Returns the {@link FreeTypeFontGenerator} for the default font, or {@code null}
   * if no default is set.
   */
  public static FreeTypeFontGenerator getDefaultGenerator() {
    if (defaultFontId == null || !entries.containsKey(defaultFontId)) {
      return null;
    }
    return entries.get(defaultFontId).getOrCreateGenerator();
  }

  /**
   * Returns a shared libGDX default {@link BitmapFont} (Arial 15px) scaled to the given
   * pixel size. Multiple {@link FlixelText} instances with the same size reuse one font.
   *
   * @param pixelSize The target font size in pixels (clamped to at least 1).
   * @return A cached bitmap font; do not {@link BitmapFont#dispose()} it — use
   *     {@link #dispose()} at shutdown.
   */
  public static BitmapFont obtainDefaultBitmapFont(int pixelSize) {
    int size = Math.max(1, pixelSize);
    BitmapFont font = defaultBitmapFontsBySize.get(size);
    if (font != null) {
      return font;
    }
    font = new BitmapFont();
    float defaultHeight = font.getLineHeight();
    if (defaultHeight > 0) {
      font.getData().setScale(size / defaultHeight);
    }
    font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    font.setUseIntegerPositions(false);
    defaultBitmapFontsBySize.put(size, font);
    return font;
  }

  /**
   * Returns a shared {@link BitmapFont} generated from the given FreeType generator with
   * the supplied parameters. Equivalent requests reuse the same instance.
   *
   * @param cacheKeyPrefix A stable prefix for this font source (e.g. {@code "reg:myId"},
   *     {@code "file:/path/font.ttf"}, {@code "def:defaultId"}).
   * @param generator The generator to use on cache miss (must match the prefix's source).
   * @param size Pixel size.
   * @param letterSpacing Horizontal spacing between characters.
   * @return A cached font; do not dispose from {@link FlixelText} it's released with the registry's
   *     {@link #dispose()} or when the corresponding font is {@link #unregister(String)}.
   */
  public static BitmapFont obtainBitmapFontFromFreeType(
      @NotNull String cacheKeyPrefix,
      @NotNull FreeTypeFontGenerator generator,
      int size,
      int letterSpacing) {
    String key = freeTypeBitmapFontKey(cacheKeyPrefix, size, letterSpacing);
    BitmapFont existing = freeTypeBitmapFonts.get(key);
    if (existing != null) {
      return existing;
    }
    FreeTypeFontParameter param = new FreeTypeFontParameter();
    param.size = size;
    param.spaceX = letterSpacing;
    param.genMipMaps = true;
    param.minFilter = Texture.TextureFilter.Linear;
    param.magFilter = Texture.TextureFilter.Linear;
    BitmapFont font = generator.generateFont(param);
    font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    freeTypeBitmapFonts.put(key, font);
    return font;
  }

  /**
   * Builds the cache key used by {@link #obtainBitmapFontFromFreeType(String,
   * FreeTypeFontGenerator, int, int)} for a given source prefix and glyph settings.
   */
  public static String freeTypeBitmapFontKey(String cacheKeyPrefix, int size, int letterSpacing) {
    return cacheKeyPrefix + "|" + size + "|" + letterSpacing;
  }

  /**
   * Disposes all cached {@link FreeTypeFontGenerator} instances and clears the
   * registry. This should be called when the game shuts down.
   */
  public static void dispose() {
    disposeAllCachedBitmapFonts();
    for (Entry entry : entries.values()) {
      entry.destroy();
    }
    entries.clear();
    defaultFontId = null;
  }

  private static void disposeAllCachedBitmapFonts() {
    for (BitmapFont font : defaultBitmapFontsBySize.values()) {
      font.dispose();
    }
    defaultBitmapFontsBySize.clear();
    for (BitmapFont font : freeTypeBitmapFonts.values()) {
      font.dispose();
    }
    freeTypeBitmapFonts.clear();
  }

  private static void removeFreeTypeBitmapFontsForPrefix(String keyPrefix) {
    Array<String> toRemove = new Array<>();
    for (String key : freeTypeBitmapFonts.keys()) {
      if (key.startsWith(keyPrefix)) {
        toRemove.add(key);
      }
    }
    for (String key : toRemove) {
      BitmapFont removed = freeTypeBitmapFonts.remove(key);
      if (removed != null) {
        removed.dispose();
      }
    }
  }

  private static Entry requireEntry(String id) {
    Entry entry = entries.get(id);
    if (entry == null) {
      throw new IllegalArgumentException("No font registered with id \"" + id + "\".");
    }
    return entry;
  }

  /** Holds the file handle and a lazily-created generator for a single registered font. */
  private static final class Entry implements FlixelDestroyable {

    final FileHandle fontFile;
    FreeTypeFontGenerator generator;

    Entry(FileHandle fontFile) {
      this.fontFile = fontFile;
    }

    FreeTypeFontGenerator getOrCreateGenerator() {
      if (generator == null) {
        generator = new FreeTypeFontGenerator(fontFile);
      }
      return generator;
    }

    @Override
    public void destroy() {
      if (generator != null) {
        generator.dispose();
        generator = null;
      }
    }
  }
}
