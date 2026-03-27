/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.debug;

import com.badlogic.gdx.Gdx;
import me.stringdotjar.flixelgdx.functional.ByteSupplier;
import me.stringdotjar.flixelgdx.functional.FloatSupplier;
import me.stringdotjar.flixelgdx.functional.ShortSupplier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

/**
 * Manages the variable watch list for the debug overlay. Watch entries are getter-based
 * (no reflection). Each entry is a display name mapped to a {@link Supplier} whose value
 * is polled every frame.
 *
 * <p>Access via {@code Flixel.watch}. The API mirrors HaxeFlixel's
 * {@code FlxG.watch} front-end as closely as possible while staying idiomatic in Java.
 *
 * <ul>
 *   <li>{@link #add(String, Supplier)}: watch a supplier (equivalent to HaxeFlixel's {@code addFunction})</li>
 *   <li>{@link #remove(String)}: remove a watched entry by name</li>
 *   <li>{@link #addMouse()} / {@link #removeMouse()}: convenience for mouse coordinates</li>
 * </ul>
 */
public class FlixelDebugWatchManager {

  private static final String MOUSE_WATCH_NAME = "Mouse";

  private final Map<String, WatchEntry> watches = new ConcurrentHashMap<>();

  /**
   * Registers a watch entry. If an entry with the same name already exists it is replaced.
   * Equivalent to HaxeFlixel's {@code FlxG.watch.addFunction}.
   *
   * @param displayName The label shown in the watch panel.
   * @param valueGetter A supplier that returns the current value each frame.
   */
  public void add(@NotNull String displayName, @NotNull Supplier<?> valueGetter) {
    if (displayName == null || valueGetter == null) {
      return;
    }
    watches.put(displayName, new ObjectWatchEntry(valueGetter));
  }

  /**
   * Registers a float watch entry without boxing.
   *
   * @param displayName The label shown in the watch panel.
   * @param valueGetter A supplier that returns a primitive float each frame.
   */
  public void add(@NotNull String displayName, @NotNull FloatSupplier valueGetter) {
    if (displayName == null || valueGetter == null) {
      return;
    }
    watches.put(displayName, new FloatWatchEntry(valueGetter));
  }

  /**
   * Registers a short watch entry without boxing.
   *
   * @param displayName The label shown in the watch panel.
   * @param valueGetter A supplier that returns a primitive short each frame.
   */
  public void add(@NotNull String displayName, @NotNull ShortSupplier valueGetter) {
    if (displayName == null || valueGetter == null) {
      return;
    }
    watches.put(displayName, new ShortWatchEntry(valueGetter));
  }

  /**
   * Registers a byte watch entry without boxing.
   *
   * @param displayName The label shown in the watch panel.
   * @param valueGetter A supplier that returns a primitive byte each frame.
   */
  public void add(@NotNull String displayName, @NotNull ByteSupplier valueGetter) {
    if (displayName == null || valueGetter == null) {
      return;
    }
    watches.put(displayName, new ByteWatchEntry(valueGetter));
  }

  /**
   * Removes a previously registered watch entry by name.
   *
   * @param displayName The label of the entry to remove.
   */
  public void remove(@NotNull String displayName) {
    if (displayName != null) {
      watches.remove(displayName);
    }
  }

  /** Adds a convenience watch entry that shows the current mouse screen position. */
  public void addMouse() {
    watches.put(MOUSE_WATCH_NAME, new ObjectWatchEntry(() -> Gdx.input.getX() + ", " + Gdx.input.getY()));
  }

  /** Removes the convenience mouse position watch entry. */
  public void removeMouse() {
    watches.remove(MOUSE_WATCH_NAME);
  }

  /**
   * Iterates every watch entry, invoking the callback with each display name and its
   * current resolved value string. No intermediate collections are created.
   *
   * @param callback Receives (displayName, currentValueString) for every entry.
   */
  public void forEach(@NotNull BiConsumer<String, String> callback) {
    for (Map.Entry<String, WatchEntry> entry : watches.entrySet()) {
      callback.accept(entry.getKey(), entry.getValue().getValueString());
    }
  }

  /** Returns {@code true} when there are no watch entries registered. */
  public boolean isEmpty() {
    return watches.isEmpty();
  }

  /** Clears all watch entries. */
  public void clear() {
    watches.clear();
  }

  /**
   * Interface for allowing primitive suppliers to be used without boxing.
   */
  private sealed interface WatchEntry permits ObjectWatchEntry, FloatWatchEntry, ShortWatchEntry, ByteWatchEntry {
    String getValueString();
  }

  private record ObjectWatchEntry(Supplier<?> supplier) implements WatchEntry {
    @Override
    public String getValueString() {
      Object val;
      try {
        val = supplier.get();
      } catch (Exception e) {
        return "<error>";
      }
      return String.valueOf(val);
    }
  }

  private record FloatWatchEntry(FloatSupplier supplier) implements WatchEntry {
    @Override
    public String getValueString() {
      try {
        float value = supplier.getAsFloat();
        return trimTrailingZeros(value);
      } catch (Exception e) {
        return "<error>";
      }
    }
  }

  private record ShortWatchEntry(ShortSupplier supplier) implements WatchEntry {
    @Override
    public String getValueString() {
      try {
        return Short.toString(supplier.getAsShort());
      } catch (Exception e) {
        return "<error>";
      }
    }
  }

  private record ByteWatchEntry(ByteSupplier supplier) implements WatchEntry {
    @Override
    public String getValueString() {
      try {
        return Byte.toString(supplier.getAsByte());
      } catch (Exception e) {
        return "<error>";
      }
    }
  }

  private static String trimTrailingZeros(float value) {
    String s = Float.toString(value);
    int dot = s.indexOf('.');
    if (dot < 0) {
      return s;
    }
    int end = s.length();
    while (end > dot + 1 && s.charAt(end - 1) == '0') {
      end--;
    }
    if (s.charAt(end - 1) == '.') {
      end--;
    }
    return s.substring(0, end);
  }
}
