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
package org.flixelgdx.locale;

import org.flixelgdx.Flixel;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.collections.FlixelSet;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.signal.FlixelSignal;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Translates text keys into the player's language, falling back to other languages when a string
 * is missing.
 *
 * <p>Reach it through {@link Flixel#locale}. Your game code asks for keys such as {@code "menu.play"}
 * instead of hard-coding {@code "Play"}, and this class answers with the text for the current
 * language. One {@link FlixelLocalizationTable} holds the strings for each language.
 *
 * <h2>The fallback chain</h2>
 *
 * <p>Picture a shelf of phrasebooks, ordered from most to least specific. When the language is
 * {@code "pt-BR"} (Brazilian Portuguese) and the fallback language is {@code "en"}, a lookup checks:
 *
 * <ol>
 *   <li>{@code pt-BR}, the exact language,</li>
 *   <li>{@code pt}, the general language, by dropping the last part of the tag,</li>
 *   <li>{@code en}, the fallback language (and its own general forms, if it has any).</li>
 * </ol>
 *
 * <p>The first phrasebook that has the key wins. Languages that were never loaded are simply skipped,
 * so you can ship a small {@code pt-BR} file with only the words that differ from {@code pt}. When no
 * phrasebook has the key, the key itself is returned (and a warning is logged once per key), which
 * keeps a missing string visible on screen instead of crashing or showing nothing.
 *
 * <h2>Example usage</h2>
 *
 * <pre>{@code
 * // During boot or a loading screen.
 * Flixel.locale.load("en", Flixel.files.internal("lang/en.json"));
 * Flixel.locale.load("es", Flixel.files.internal("lang/es.json"));
 * Flixel.locale.setLocale("es");
 *
 * // Plain text, for example when building a menu.
 * playButton.setText(Flixel.locale.get("menu.play"));
 *
 * // Text with values, every frame, without garbage.
 * // es.json contains: "hud": { "score": "Puntos: {0}" }
 * private final FlixelString scoreValue = new FlixelString(16);
 * private final FlixelString scoreLine = new FlixelString(32);
 *
 * @Override
 * public void update(float elapsed) {
 *   super.update(elapsed);
 *   scoreValue.set(score);
 *   scoreText.setText(Flixel.locale.format(scoreLine, "hud.score", scoreValue));
 * }
 * }</pre>
 *
 * <p>Language tags follow the usual {@code language-REGION} shape ({@code "en"}, {@code "en-US"},
 * {@code "zh-Hant-TW"}). Underscores are turned into hyphens, so {@code "pt_BR"} and {@code "pt-BR"}
 * mean the same thing, but letter case is kept as given, so pick one style and use it everywhere.
 *
 * <p>Lookups and formatting do not allocate. Loading files and changing the language do, so do those
 * outside per-frame code. Use this class from the main game thread only.
 *
 * @see FlixelLocalizationTable
 * @see FlixelString#setFormat(CharSequence, CharSequence...)
 */
public class FlixelLocalization {

  /** The language tag used before {@link #setLocale(String)} is called, and the default fallback. */
  public static final String DEFAULT_TAG = "en";

  /**
   * Dispatched with the new language tag after {@link #setLocale(String)} changes the language.
   *
   * <p>Listen to this to refresh labels that were set once and are not rebuilt every frame.
   */
  public final FlixelSignal<String> onLocaleChanged = new FlixelSignal<>();

  @NotNull
  private final FlixelMap<String, FlixelLocalizationTable> tables = new FlixelMap<>();

  /** The tables to search, most specific first. Rebuilt whenever the language or loaded tables change. */
  @NotNull
  private final FlixelArray<FlixelLocalizationTable> chain = new FlixelArray<>(4);

  /** Keys that were already reported as missing, so each one is only logged once. */
  @NotNull
  private final FlixelSet<String> reported = new FlixelSet<>();

  @NotNull
  private String locale = DEFAULT_TAG;

  @NotNull
  private String fallback = DEFAULT_TAG;

  /** Whether missing keys are reported through {@link Flixel#log}. */
  private boolean warnMissing = true;

  /**
   * Reads a JSON file and adds its strings to the table for {@code tag}.
   *
   * <p>If a table for {@code tag} already exists, the new strings are merged into it and override
   * matching keys. This lets you split a language over several files, such as {@code menus.json}
   * and {@code dialogue.json}.
   *
   * @param tag The language tag the file belongs to, such as {@code "es"}.
   * @param file The JSON file to read. Its root must be an object.
   * @return The table that received the strings.
   * @throws IllegalArgumentException If the file is missing, malformed, or its root is not an object.
   */
  @NotNull
  public FlixelLocalizationTable load(@NotNull String tag, @NotNull FlixelFile file) {
    if (!file.exists()) {
      throw new IllegalArgumentException("Localization file not found: " + file.getPath());
    }
    return loadJson(tag, file.readString());
  }

  /**
   * Parses JSON text and adds its strings to the table for {@code tag}.
   *
   * <p>Works like {@link #load(String, FlixelFile)} for text you already have in memory.
   *
   * @param tag The language tag the strings belong to.
   * @param json The JSON text. Its root must be an object.
   * @return The table that received the strings.
   * @throws IllegalArgumentException If the JSON is malformed or its root is not an object.
   */
  @NotNull
  public FlixelLocalizationTable loadJson(@NotNull String tag, @NotNull CharSequence json) {
    String key = normalize(tag);
    FlixelLocalizationTable table = tables.get(key);
    boolean created = table == null;
    if (created) {
      table = new FlixelLocalizationTable(key);
    }
    table.putJson(json);
    if (created) {
      tables.put(key, table);
      rebuildChain();
    }
    reported.clear();
    return table;
  }

  /**
   * Adds a table, replacing any table already registered for the same language tag.
   *
   * @param table The table to add.
   */
  public void add(@NotNull FlixelLocalizationTable table) {
    tables.put(table.getTag(), table);
    reported.clear();
    rebuildChain();
  }

  /**
   * Removes the table for {@code tag}.
   *
   * @param tag The language tag to remove.
   * @return The removed table, or {@code null} when none was registered.
   */
  @Nullable
  public FlixelLocalizationTable remove(@NotNull String tag) {
    FlixelLocalizationTable removed = tables.remove(normalize(tag));
    if (removed != null) {
      rebuildChain();
    }
    return removed;
  }

  /** Removes every table. The current and fallback language tags are kept. */
  public void clear() {
    tables.clear();
    reported.clear();
    chain.clear();
  }

  /**
   * Returns the translated text for {@code key}, searching the fallback chain.
   *
   * @param key The key to look up, such as {@code "menu.play"}.
   * @return The translated text, or {@code key} itself when no table has it.
   */
  @NotNull
  public String get(@NotNull String key) {
    String text = find(key);
    if (text != null) {
      return text;
    }
    reportMissing(key);
    return key;
  }

  /**
   * Returns the translated text for {@code key}, or {@code defaultValue} when no table has it.
   *
   * <p>Unlike {@link #get(String)}, a miss here is expected, so nothing is logged.
   *
   * @param key The key to look up.
   * @param defaultValue What to return when the key is missing.
   * @return The translated text, or {@code defaultValue}.
   */
  @Nullable
  public String get(@NotNull String key, @Nullable String defaultValue) {
    String text = find(key);
    return text != null ? text : defaultValue;
  }

  /**
   * Returns whether any table in the fallback chain has {@code key}.
   *
   * @param key The key to check.
   * @return {@code true} if a translation exists.
   */
  public boolean has(@NotNull String key) {
    return find(key) != null;
  }

  /**
   * Writes the translated text for {@code key} into {@code out}, replacing its content.
   *
   * <p>Useful when the text has no placeholders but you want to reuse one buffer.
   *
   * @param out The buffer to write into.
   * @param key The key to look up.
   * @return {@code out}, for passing straight to a text object.
   */
  @NotNull
  public FlixelString format(@NotNull FlixelString out, @NotNull String key) {
    return out.set(get(key));
  }

  /**
   * Writes the translated text for {@code key} into {@code out} with {@code {0}} filled in.
   *
   * @param out The buffer to write into. It is cleared first.
   * @param key The key to look up.
   * @param a0 The value for {@code {0}}.
   * @return {@code out}, for passing straight to a text object.
   * @see FlixelString#setFormat(CharSequence, CharSequence...)
   */
  @NotNull
  public FlixelString format(@NotNull FlixelString out, @NotNull String key, @Nullable CharSequence a0) {
    return out.setFormat(get(key), a0);
  }

  /**
   * Writes the translated text for {@code key} into {@code out} with {@code {0}} and {@code {1}} filled in.
   *
   * @param out The buffer to write into. It is cleared first.
   * @param key The key to look up.
   * @param a0 The value for {@code {0}}.
   * @param a1 The value for {@code {1}}.
   * @return {@code out}, for passing straight to a text object.
   */
  @NotNull
  public FlixelString format(
      @NotNull FlixelString out, @NotNull String key, @Nullable CharSequence a0, @Nullable CharSequence a1) {
    return out.setFormat(get(key), a0, a1);
  }

  /**
   * Writes the translated text for {@code key} into {@code out} with {@code {0}} to {@code {2}} filled in.
   *
   * @param out The buffer to write into. It is cleared first.
   * @param key The key to look up.
   * @param a0 The value for {@code {0}}.
   * @param a1 The value for {@code {1}}.
   * @param a2 The value for {@code {2}}.
   * @return {@code out}, for passing straight to a text object.
   */
  @NotNull
  public FlixelString format(
      @NotNull FlixelString out,
      @NotNull String key,
      @Nullable CharSequence a0,
      @Nullable CharSequence a1,
      @Nullable CharSequence a2) {
    return out.setFormat(get(key), a0, a1, a2);
  }

  /**
   * Writes the translated text for {@code key} into {@code out} with every placeholder filled in.
   *
   * <p>This overload allocates an array on every call; prefer the fixed overloads in per-frame code.
   *
   * @param out The buffer to write into. It is cleared first.
   * @param key The key to look up.
   * @param args The values for {@code {0}}, {@code {1}}, and so on.
   * @return {@code out}, for passing straight to a text object.
   */
  @NotNull
  public FlixelString format(@NotNull FlixelString out, @NotNull String key, @Nullable CharSequence... args) {
    return out.setFormat(get(key), args);
  }

  /**
   * Turns a language tag into the form used for lookups: trimmed, with underscores replaced by hyphens.
   *
   * @param tag The tag to normalize.
   * @return The normalized tag.
   */
  @NotNull
  static String normalize(@NotNull String tag) {
    return tag.trim().replace('_', '-');
  }

  @Nullable
  private String find(@NotNull String key) {
    for (int i = 0; i < chain.getSize(); i++) {
      String text = chain.get(i).get(key);
      if (text != null) {
        return text;
      }
    }
    return null;
  }

  private void reportMissing(@NotNull String key) {
    if (warnMissing && reported.add(key)) {
      Flixel.log.warn("Missing localization key \"{}\" for locale \"{}\".", key, locale);
    }
  }

  private void rebuildChain() {
    chain.clear();
    appendTagChain(locale);
    appendTagChain(fallback);
  }

  /** Adds the tables for {@code tag} and each shorter form of it, such as {@code zh-Hant-TW}, {@code zh-Hant}, {@code zh}. */
  private void appendTagChain(@NotNull String tag) {
    String current = tag;
    while (!current.isEmpty()) {
      FlixelLocalizationTable table = tables.get(current);
      if (table != null && !chain.contains(table, true)) {
        chain.add(table);
      }
      int dash = current.lastIndexOf('-');
      if (dash < 0) {
        break;
      }
      current = current.substring(0, dash);
    }
  }

  /**
   * Returns the current language tag.
   *
   * @return The tag, such as {@code "es"}.
   */
  @NotNull
  public String getLocale() {
    return locale;
  }

  /**
   * Switches the language and dispatches {@link #onLocaleChanged} if it actually changed.
   *
   * <p>The language does not need to be loaded yet; tables loaded later join the fallback chain
   * automatically. This allocates, so do not call it every frame.
   *
   * @param tag The new language tag, such as {@code "es"} or {@code "pt-BR"}.
   */
  public void setLocale(@NotNull String tag) {
    String next = normalize(tag);
    if (next.equals(locale)) {
      return;
    }
    locale = next;
    reported.clear();
    rebuildChain();
    onLocaleChanged.dispatch(next);
  }

  /**
   * Returns the language searched after the current one runs out.
   *
   * @return The fallback tag. Defaults to {@link #DEFAULT_TAG}.
   */
  @NotNull
  public String getFallback() {
    return fallback;
  }

  /**
   * Sets the language searched after the current one runs out, usually the language the game was
   * written in.
   *
   * @param tag The fallback language tag.
   */
  public void setFallback(@NotNull String tag) {
    fallback = normalize(tag);
    reported.clear();
    rebuildChain();
  }

  /**
   * Returns the table registered for {@code tag}.
   *
   * @param tag The language tag.
   * @return The table, or {@code null} when that language was never loaded.
   */
  @Nullable
  public FlixelLocalizationTable getTable(@NotNull String tag) {
    return tables.get(normalize(tag));
  }

  public boolean getWarnMissing() {
    return warnMissing;
  }

  /**
   * Sets whether {@link #get(String)} logs a warning the first time each missing key is requested.
   *
   * @param warnMissing {@code true} to log missing keys, which is the default.
   */
  public void setWarnMissing(boolean warnMissing) {
    this.warnMissing = warnMissing;
  }
}
