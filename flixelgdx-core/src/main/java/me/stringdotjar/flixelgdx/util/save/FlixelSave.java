/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.util.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;

import me.stringdotjar.flixelgdx.FlixelDestroyable;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class that provides a high-level, cross-platform mechanism for saving and
 * loading persistent game data within the FlixelGDX framework using libGDX's Preferences API.
 *
 * <p>This utility abstracts away the complexities of serialization and platform-specific details,
 * allowing you to bind to a uniquely-identified save slot, manipulate a structured {@link ObjectMap}
 * of key-value pairs (which can contain nested structures compatible with libGDX's {@link Json} API),
 * and safely flush or clear saved progress.
 *
 * <h2>Key Features:</h2>
 * <ul>
 *     <li><b>Slot-based Save Management:</b> Bind to a specific save "slot" using {@link #bind}, supporting multiple profiles or manual slots.</li>
 *     <li><b>Automatic JSON Serialization:</b> All data stored is serialized to JSON transparently via {@link Json}, enabling nested objects, numeric arrays, etc.</li>
 *     <li><b>Safe</b>: Handles error states and empty preferences files systematically—check {@link #getStatus()} and {@link #isBound()} for reliability.</li>
 *     <li><b>Merging and Flushing:</b> Easily merge external data with conflict resolution, and flush changes immediately to disk.</li>
 *     <li><b>Destroy/Clear:</b> Data can be programmatically wiped for a "reset save" experience.</li>
 * </ul>
 *
 * <h2>Typical Usage:</h2>
 * <pre>{@code
 * FlixelSave save = new FlixelSave();
 * save.bind("savefile", null); // or ("savefile", "slot1")
 * save.data.put("score", 12345);
 * save.flush();
 *
 * int highScore = (int)save.data.get("score", 0);
 * }</pre>
 *
 * <h2>Threading and Platform Notes:</h2>
 * <ul>
 *     <li>This class operates on the main thread. Use after libGDX's application context is initialized.</li>
 *     <li>Data is written using {@link Preferences#flush()} and may be lost if not explicitly flushed.</li>
 *     <li>All keys and nested data should be JSON-serializable and compatible with libGDX's expectations.</li>
 *     <li>Intended for small to medium-sized game progress data, not binary assets or large files.</li>
 * </ul>
 *
 * @see com.badlogic.gdx.Preferences
 * @see com.badlogic.gdx.utils.Json
 * @see com.badlogic.gdx.utils.ObjectMap
 * @see FlixelSaveStatus
 */

public class FlixelSave implements FlixelDestroyable {

  private static final String DATA_KEY = "flixelgdx.save.data";

  private final Json json = new Json();

  @Nullable
  private Preferences preferences;

  @NotNull
  private String boundName = "";

  @Nullable
  private String boundSlot;

  private boolean bound;

  @NotNull
  private FlixelSaveStatus status = FlixelSaveStatus.EMPTY;

  /** Root data object (JSON-compatible tree via libGDX {@link ObjectMap}). */
  @NotNull
  private final ObjectMap<String, Object> data = new ObjectMap<>();

  /**
   * Binds to local preferences. {@code slot} selects a separate file name suffix for multiple slots.
   *
   * @param name primary preferences name (no spaces; safe for file names)
   * @param slot optional suffix, e.g. {@code "slot1"} -> {@code name_slot1}
   * @return {@code true} if bind succeeded and data was loaded (or empty new save)
   */
  public boolean bind(@NotNull String name, @Nullable String slot) {
    if (name.isEmpty()) {
      status = FlixelSaveStatus.ERROR;
      bound = false;
      return false;
    }
    boundName = name;
    boundSlot = slot;
    String prefName = slot != null && !slot.isEmpty() ? name + "_" + slot : name;
    preferences = Gdx.app.getPreferences(prefName);
    bound = true;
    status = FlixelSaveStatus.OK;
    load();
    return true;
  }

  public boolean isBound() {
    return bound && preferences != null;
  }

  @NotNull
  public String getName() {
    return boundName;
  }

  @Nullable
  public String getSlot() {
    return boundSlot;
  }

  @NotNull
  public FlixelSaveStatus getStatus() {
    return status;
  }

  @NotNull
  public ObjectMap<String, Object> getData() {
    return data;
  }

  /** Reloads from {@link Preferences} into {@link #getData()}. */
  public void load() {
    if (preferences == null) {
      return;
    }
    data.clear();
    String raw = preferences.getString(DATA_KEY, "");
    if (raw.isEmpty()) {
      legacyMergeFlatStringKeys();
      return;
    }
    try {
      @SuppressWarnings("unchecked")
      ObjectMap<String, Object> parsed = json.fromJson(ObjectMap.class, raw);
      if (parsed != null) {
        data.putAll(parsed);
      }
    } catch (Exception e) {
      status = FlixelSaveStatus.ERROR;
    }

  }

  /** Merges legacy flat string keys (excluding blob key) into {@link #data} for backwards compatibility. */
  private void legacyMergeFlatStringKeys() {
    if (preferences == null) {
      return;
    }
    Map<String, ?> all = preferences.get();
    if (all == null) {
      return;
    }
    for (Map.Entry<String, ?> e : all.entrySet()) {
      if (DATA_KEY.equals(e.getKey())) {
        continue;
      }
      Object v = e.getValue();
      data.put(e.getKey(), v != null ? v.toString() : "");
    }
  }

  /** Writes {@link #getData()} to preferences and flushes. */
  public boolean flush() {
    if (preferences == null) {
      return false;
    }
    try {
      String serialized = json.toJson(data);
      preferences.putString(DATA_KEY, serialized);
      preferences.flush();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /** Erases the save data and flushes automatically. */
  public boolean erase() {
    if (preferences == null) {
      return false;
    }
    data.clear();
    preferences.clear();
    preferences.flush();
    return true;
  }

  public boolean isEmpty() {
    return data.size == 0;
  }

  @Override
  public void destroy() {
    data.clear();
    preferences = null;
    bound = false;
    boundName = "";
    boundSlot = null;
    status = FlixelSaveStatus.EMPTY;
  }

  /**
   * Merges another data map into this save (optionally overwriting keys).
   *
   * @param source The data map to merge into this save.
   * @param overwrite Whether to overwrite existing keys.
   * @param flushAfter Whether to flush the data after merging.
   * @return {@code true} if data changed and flush succeeded when {@code flushAfter} is true.
   */
  public boolean mergeData(@NotNull ObjectMap<String, Object> source, boolean overwrite, boolean flushAfter) {
    boolean changed = false;
    for (ObjectMap.Entry<String, Object> e : source.entries()) {
      if (overwrite || !data.containsKey(e.key)) {
        data.put(e.key, e.value);
        changed = true;
      }
    }
    if (changed && flushAfter) {
      return flush();
    }
    return changed;
  }

  public boolean close(int minFileSize) {
    return flush();
  }
}
