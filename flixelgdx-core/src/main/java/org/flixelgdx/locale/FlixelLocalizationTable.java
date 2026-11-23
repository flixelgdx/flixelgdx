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

import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.json.FlixelJson;
import org.flixelgdx.json.FlixelJsonValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Every translated string for one language, looked up by key.
 *
 * <p>Think of a localization table as one page of a phrasebook: the left column holds keys your code uses
 * (such as {@code "menu.play"}) and the right column holds what the player reads in this language
 * (such as {@code "Jugar"}). Your code only ever names the key, so switching language means swapping
 * the page, not rewriting the game.
 *
 * <p>Tables are usually loaded from JSON through {@link FlixelLocalization#load(String, FlixelFile)},
 * but you can also build one by hand or with {@link #putJson(CharSequence)}. Nested JSON objects are
 * flattened into dotted keys, and arrays use their index as the last part of the key:
 *
 * <pre>{@code
 * {
 *   "menu": { "play": "Play", "quit": "Quit" },
 *   "hud": { "score": "Score: {0}" },
 *   "intro": [ "Long ago...", "A hero woke up." ]
 * }
 * }</pre>
 *
 * <p>That file produces the keys {@code menu.play}, {@code menu.quit}, {@code hud.score},
 * {@code intro.0}, and {@code intro.1}. Numbers and booleans are stored as text, and {@code null}
 * values are skipped.
 *
 * <p>Lookups with {@link #get(String)} do not allocate. Loading does, so load tables during boot or a
 * loading screen, not every frame.
 */
public final class FlixelLocalizationTable {

  @NotNull
  private final String tag;

  @NotNull
  private final FlixelMap<String, String> entries;

  /**
   * Creates an empty table for the given language tag.
   *
   * @param tag The language tag, such as {@code "en"} or {@code "pt-BR"}. Underscores are treated as hyphens.
   */
  public FlixelLocalizationTable(@NotNull String tag) {
    this.tag = FlixelLocalization.normalize(tag);
    this.entries = new FlixelMap<>();
  }

  /**
   * Stores one translated string, replacing any earlier value for the same key.
   *
   * @param key The key your code looks the string up by.
   * @param value The translated text.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelLocalizationTable put(@NotNull String key, @NotNull String value) {
    entries.put(key, value);
    return this;
  }

  /**
   * Parses JSON text and adds every string in it to this table.
   *
   * <p>Keys that already exist are overwritten, so a language can be split across several files and
   * loaded in order.
   *
   * @param json The JSON text. Its root must be an object.
   * @return {@code this} for chaining.
   * @throws IllegalArgumentException If the JSON is malformed or its root is not an object.
   */
  @NotNull
  public FlixelLocalizationTable putJson(@NotNull CharSequence json) {
    FlixelJsonValue root = FlixelJson.parse(json.toString());
    if (!root.isObject()) {
      throw new IllegalArgumentException(
          "A localization table's JSON root must be an object, but it was " + root.getKind() + ".");
    }
    putJson(root);
    return this;
  }

  /**
   * Adds every string in an already parsed JSON object to this table.
   *
   * @param root The JSON object to read. Non-object values add nothing.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelLocalizationTable putJson(@NotNull FlixelJsonValue root) {
    if (root.isObject()) {
      flatten(root, null);
    }
    return this;
  }

  /**
   * Returns the translated text for {@code key}, or {@code null} when this table does not have it.
   *
   * @param key The key to look up.
   * @return The text, or {@code null} when missing.
   */
  @Nullable
  public String get(@NotNull String key) {
    return entries.get(key);
  }

  /**
   * Returns whether this table has a string for {@code key}.
   *
   * @param key The key to check.
   * @return {@code true} if the key exists.
   */
  public boolean has(@NotNull String key) {
    return entries.containsKey(key);
  }

  /**
   * Removes one string from this table.
   *
   * @param key The key to remove.
   * @return The removed text, or {@code null} when the key was missing.
   */
  @Nullable
  public String remove(@NotNull String key) {
    return entries.remove(key);
  }

  /** Removes every string from this table. */
  public void clear() {
    entries.clear();
  }

  private void flatten(@NotNull FlixelJsonValue node, @Nullable String prefix) {
    boolean array = node.isArray();
    for (int i = 0; i < node.getSize(); i++) {
      FlixelJsonValue child = node.get(i);
      if (child == null || child.isNull()) {
        continue;
      }
      String part = array ? Integer.toString(i) : child.getName();
      if (part == null) {
        continue;
      }
      String key = prefix == null ? part : prefix + '.' + part;
      if (child.isObject() || child.isArray()) {
        flatten(child, key);
      } else if (child.getKind() == FlixelJsonValue.Kind.NUMBER) {
        double d = child.asDouble();
        long whole = (long) d;
        entries.put(key, whole == d ? Long.toString(whole) : Double.toString(d));
      } else {
        String text = child.asString();
        if (text != null) {
          entries.put(key, text);
        }
      }
    }
  }

  /**
   * Returns the normalized language tag this table belongs to.
   *
   * @return The tag, such as {@code "pt-BR"}.
   */
  @NotNull
  public String getTag() {
    return tag;
  }

  /**
   * Returns how many strings this table holds.
   *
   * @return The number of keys.
   */
  public int getSize() {
    return entries.getSize();
  }
}
