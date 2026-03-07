package me.stringdotjar.flixelgdx.text;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
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
 *
 * <h3>Usage</h3>
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
 * <h3>Lifecycle</h3>
 * <p>{@link #dispose()} is called when the game shuts down (in
 * {@code FlixelGame.dispose()}) to release all cached generators. Individual entries
 * can be removed earlier with {@link #unregister(String)}.
 */
public final class FlixelFontRegistry {

  private static final Map<String, Entry> entries = new HashMap<>();

  /** The ID of the font that FlixelText uses when no explicit font is set. */
  private static String defaultFontId;

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
      existing.dispose();
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
    Entry removed = entries.remove(id);
    if (removed != null) {
      removed.dispose();
    }
    if (id != null && id.equals(defaultFontId)) {
      defaultFontId = null;
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
   * Disposes all cached {@link FreeTypeFontGenerator} instances and clears the
   * registry. This should be called when the game shuts down.
   */
  public static void dispose() {
    for (Entry entry : entries.values()) {
      entry.dispose();
    }
    entries.clear();
    defaultFontId = null;
  }

  private static Entry requireEntry(String id) {
    Entry entry = entries.get(id);
    if (entry == null) {
      throw new IllegalArgumentException("No font registered with id \"" + id + "\".");
    }
    return entry;
  }

  /** Holds the file handle and a lazily-created generator for a single registered font. */
  private static final class Entry {

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

    void dispose() {
      if (generator != null) {
        generator.dispose();
        generator = null;
      }
    }
  }
}
