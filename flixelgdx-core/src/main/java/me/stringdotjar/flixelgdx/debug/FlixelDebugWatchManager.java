/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.functional.supplier.ByteSupplier;
import me.stringdotjar.flixelgdx.functional.supplier.CharSupplier;
import me.stringdotjar.flixelgdx.functional.supplier.FloatSupplier;
import me.stringdotjar.flixelgdx.functional.supplier.ShortSupplier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
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
   * Registers a {@code byte}-valued watch entry without boxing.
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
   * Registers a {@code short}-valued watch entry without boxing.
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
   * Registers an {@code int}-valued watch entry without boxing.
   *
   * @param displayName The label shown in the watch panel.
   * @param valueGetter A supplier that returns a primitive int each frame.
   */
  public void add(@NotNull String displayName, @NotNull IntSupplier valueGetter) {
    if (displayName == null || valueGetter == null) {
      return;
    }
    watches.put(displayName, new IntWatchEntry(valueGetter));
  }

  /**
   * Registers a {@code long}-valued watch entry without boxing.
   *
   * @param displayName The label shown in the watch panel.
   * @param valueGetter A supplier that returns a primitive long each frame.
   */
  public void add(@NotNull String displayName, @NotNull LongSupplier valueGetter) {
    if (displayName == null || valueGetter == null) {
      return;
    }
    watches.put(displayName, new LongWatchEntry(valueGetter));
  }

  /**
   * Registers a {@code float}-valued watch entry without boxing.
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
   * Registers a {@code double}-valued watch entry without boxing.
   *
   * @param displayName The label shown in the watch panel.
   * @param valueGetter A supplier that returns a primitive double each frame.
   */
  public void add(@NotNull String displayName, @NotNull DoubleSupplier valueGetter) {
    if (displayName == null || valueGetter == null) {
      return;
    }
    watches.put(displayName, new DoubleWatchEntry(valueGetter));
  }

  /**
   * Registers a {@code boolean}-valued watch entry without boxing.
   *
   * @param displayName The label shown in the watch panel.
   * @param valueGetter A supplier that returns a primitive boolean each frame.
   */
  public void add(@NotNull String displayName, @NotNull BooleanSupplier valueGetter) {
    if (displayName == null || valueGetter == null) {
      return;
    }
    watches.put(displayName, new BooleanWatchEntry(valueGetter));
  }

  /**
   * Registers a {@code char}-valued watch entry without boxing.
   *
   * @param displayName The label shown in the watch panel.
   * @param valueGetter A supplier that returns a primitive char each frame.
   */
  public void add(@NotNull String displayName, @NotNull CharSupplier valueGetter) {
    if (displayName == null || valueGetter == null) {
      return;
    }
    watches.put(displayName, new CharWatchEntry(valueGetter));
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
    watches.put(
      MOUSE_WATCH_NAME,
      new ObjectWatchEntry(() -> {
        if (Flixel.mouse != null) {
          return Flixel.mouse.getScreenX() + ", " + Flixel.mouse.getScreenY();
        }
        return Gdx.input.getX() + ", " + Gdx.input.getY();
      }));
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

  /**
   * Formats all watch lines into {@code output}, reusing existing {@link StringBuilder} slots and
   * shrinking the array when fewer watches are registered. Each line includes markup for the debug overlay.
   *
   * @param output Cleared and filled with one builder per watch entry (order follows {@link Map#entrySet()}).
   */
  public void fillWatchLineBuilders(@NotNull Array<StringBuilder> output) {
    int n = watches.size();
    while (output.size < n) {
      output.add(new StringBuilder(64));
    }
    output.setSize(n);
    int i = 0;
    for (Map.Entry<String, WatchEntry> entry : watches.entrySet()) {
      StringBuilder sb = output.get(i++);
      sb.setLength(0);
      sb.append("[#88CCFF]").append(entry.getKey()).append(":[#FFFFFF] ");
      sb.append(entry.getValue().getValueString());
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
  protected interface WatchEntry {

    /**
     * Returns the current value of the watch entry as a string.
     *
     * @return The current value of the watch entry as a string. May be {@code "<error>"} if
     *   the supplier throws an exception.
     */
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

  private record IntWatchEntry(IntSupplier supplier) implements WatchEntry {

    @Override
    public String getValueString() {
      try {
        return Integer.toString(supplier.getAsInt());
      } catch (Exception e) {
        return "<error>";
      }
    }
  }

  private record FloatWatchEntry(FloatSupplier supplier) implements WatchEntry {

    @Override
    public String getValueString() {
      try {
        return Float.toString(supplier.getAsFloat());
      } catch (Exception e) {
        return "<error>";
      }
    }
  }

  private record LongWatchEntry(LongSupplier supplier) implements WatchEntry {

    @Override
    public String getValueString() {
      try {
        return Long.toString(supplier.getAsLong());
      } catch (Exception e) {
        return "<error>";
      }
    }
  }

  private record DoubleWatchEntry(DoubleSupplier supplier) implements WatchEntry {

    @Override
    public String getValueString() {
      try {
        return Double.toString(supplier.getAsDouble());
      } catch (Exception e) {
        return "<error>";
      }
    }
  }

  private record BooleanWatchEntry(BooleanSupplier supplier) implements WatchEntry {

    @Override
    public String getValueString() {
      return Boolean.toString(supplier.getAsBoolean());
    }
  }

  private record CharWatchEntry(CharSupplier supplier) implements WatchEntry {

    @Override
    public String getValueString() {
      try {
        return Character.toString(supplier.getAsChar());
      } catch (Exception e) {
        return "<error>";
      }
    }
  }
}
