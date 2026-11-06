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
package org.flixelgdx.debug;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.functional.supplier.ByteSupplier;
import org.flixelgdx.functional.supplier.CharSupplier;
import org.flixelgdx.functional.supplier.FloatSupplier;
import org.flixelgdx.functional.supplier.ShortSupplier;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * The watch panel of the remote debugger: a list of named, live values that you want to keep an eye on
 * while the game runs.
 *
 * <p>Think of it as sticky notes on a monitor. Each note has a name ("Player X") and a little
 * function that reads the current value. Every time the debugger app asks, each function is read once
 * and the answer is sent along with its type, so the app can show integers, decimals, and flags the
 * way they deserve. Nothing is read when no app is listening, so watches cost almost nothing.
 *
 * <p>Use the typed methods for primitives. They avoid boxing and tell the app the exact type:
 *
 * <pre>{@code
 * Flixel.debug.watches.addFloat("Player X", player::getX);
 * Flixel.debug.watches.addInt("Score", () -> score);
 * Flixel.debug.watches.addBoolean("On ground", player::isTouchingFloor);
 * Flixel.debug.watches.add("State", () -> machine.getCurrent().getName()); // Any object.
 *
 * Flixel.debug.watches.remove("Player X");
 * }</pre>
 *
 * <p>If a getter throws, the app shows the error message in place of the value, and the other watches keep
 * working. Adding a watch with a name that already exists replaces the old one. All methods must be called
 * from the game's main thread.
 *
 * @see FlixelDebugProtocol#KIND_WATCHES
 */
public class FlixelDebugWatchPanel extends FlixelDebugPanelBase {

  /** The name of the watch added by {@link #addMouse()}. */
  public static final String MOUSE_NAME = "Mouse";

  /** The name of the watch added by {@link #addCamera()}. */
  public static final String CAMERA_NAME = "Camera";

  /** Seconds between revision bumps, which is how often the app is offered fresh values. */
  private static final float REFRESH_INTERVAL = 0.1f;

  private final FlixelArray<Watch> watches = new FlixelArray<>();
  private final FlixelString scratch = new FlixelString(64);

  private float timer;

  /** Creates the watch panel. */
  public FlixelDebugWatchPanel() {
    super("watches", "Watches", FlixelDebugProtocol.KIND_WATCHES);
  }

  /**
   * Watches an object value. The value is sent as a number, flag, or text depending on its type, and
   * any other object is sent as the text of its {@code toString()}.
   *
   * @param name The label shown in the app. An existing watch with this name is replaced.
   * @param getter A function that returns the current value.
   */
  public void add(@NotNull String name, @NotNull Supplier<?> getter) {
    put(name, getter, new ObjectWatch(name, getter));
  }

  /**
   * Watches an {@code int} value without boxing.
   *
   * @param name The label shown in the app. An existing watch with this name is replaced.
   * @param getter A function that returns the current value.
   */
  public void addInt(@NotNull String name, @NotNull IntSupplier getter) {
    put(name, getter, new IntWatch(name, getter));
  }

  /**
   * Watches a {@code long} value without boxing.
   *
   * @param name The label shown in the app. An existing watch with this name is replaced.
   * @param getter A function that returns the current value.
   */
  public void addLong(@NotNull String name, @NotNull LongSupplier getter) {
    put(name, getter, new LongWatch(name, getter));
  }

  /**
   * Watches a {@code float} value without boxing.
   *
   * @param name The label shown in the app. An existing watch with this name is replaced.
   * @param getter A function that returns the current value.
   */
  public void addFloat(@NotNull String name, @NotNull FloatSupplier getter) {
    put(name, getter, new FloatWatch(name, getter));
  }

  /**
   * Watches a {@code double} value without boxing.
   *
   * @param name The label shown in the app. An existing watch with this name is replaced.
   * @param getter A function that returns the current value.
   */
  public void addDouble(@NotNull String name, @NotNull DoubleSupplier getter) {
    put(name, getter, new DoubleWatch(name, getter));
  }

  /**
   * Watches a {@code boolean} value without boxing.
   *
   * @param name The label shown in the app. An existing watch with this name is replaced.
   * @param getter A function that returns the current value.
   */
  public void addBoolean(@NotNull String name, @NotNull BooleanSupplier getter) {
    put(name, getter, new BooleanWatch(name, getter));
  }

  /**
   * Watches a {@code byte} value without boxing. It is sent as an integer.
   *
   * @param name The label shown in the app. An existing watch with this name is replaced.
   * @param getter A function that returns the current value.
   */
  public void addByte(@NotNull String name, @NotNull ByteSupplier getter) {
    put(name, getter, new ByteWatch(name, getter));
  }

  /**
   * Watches a {@code short} value without boxing. It is sent as an integer.
   *
   * @param name The label shown in the app. An existing watch with this name is replaced.
   * @param getter A function that returns the current value.
   */
  public void addShort(@NotNull String name, @NotNull ShortSupplier getter) {
    put(name, getter, new ShortWatch(name, getter));
  }

  /**
   * Watches a {@code char} value without boxing. It is sent as a one-character text.
   *
   * @param name The label shown in the app. An existing watch with this name is replaced.
   * @param getter A function that returns the current value.
   */
  public void addChar(@NotNull String name, @NotNull CharSupplier getter) {
    put(name, getter, new CharWatch(name, getter));
  }

  /** Adds a watch named {@value #MOUSE_NAME} that shows the mouse position in screen pixels. */
  public void addMouse() {
    put(MOUSE_NAME, MOUSE_NAME, new MouseWatch());
  }

  /** Adds a watch named {@value #CAMERA_NAME} that shows the scroll position and zoom of the first camera. */
  public void addCamera() {
    put(CAMERA_NAME, CAMERA_NAME, new CameraWatch());
  }

  /**
   * Removes a watch.
   *
   * @param name The label of the watch to remove. An unknown name does nothing.
   * @return {@code true} when a watch was removed.
   */
  public boolean remove(@Nullable String name) {
    int index = indexOf(name);
    if (index < 0) {
      return false;
    }
    watches.removeIndex(index);
    markChanged();
    return true;
  }

  /** Removes the watch added by {@link #addMouse()}. */
  public void removeMouse() {
    remove(MOUSE_NAME);
  }

  /** Removes the watch added by {@link #addCamera()}. */
  public void removeCamera() {
    remove(CAMERA_NAME);
  }

  /** Removes every watch, including the mouse and camera watches. */
  public void clear() {
    if (watches.getSize() > 0) {
      watches.clear();
      markChanged();
    }
  }

  /**
   * Returns whether a watch with the given label exists.
   *
   * @param name The label to look for.
   * @return {@code true} when the watch exists.
   */
  public boolean has(@Nullable String name) {
    return indexOf(name) >= 0;
  }

  @Override
  public void update(float elapsed) {
    timer += elapsed;
    if (timer >= REFRESH_INTERVAL) {
      timer = 0f;
      if (watches.getSize() > 0) {
        markChanged();
      }
    }
  }

  @Override
  public void write(@NotNull FlixelDebugMessageWriter writer) {
    int n = Math.min(watches.getSize(), FlixelDebugProtocol.MAX_ARRAY_COUNT);
    writer.beginArray();
    for (int i = 0; i < n; i++) {
      Watch watch = watches.get(i);
      writer.writeString(watch.name);
      try {
        watch.writeValue(writer, scratch);
      } catch (Exception e) {
        String message = e.getMessage();
        writer.writeErrorValue(message != null ? message : e.getClass().getName());
      }
    }
    writer.endArray(n);
  }

  @Override
  public void reset() {
    clear();
  }

  /**
   * Appends a number for display: whole numbers without a decimal point, anything else with two decimals.
   *
   * @param out The buffer to append to.
   * @param value The number to format.
   */
  public static void appendNumber(@NotNull FlixelString out, double value) {
    if (value == Math.rint(value) && Math.abs(value) < 1e15) {
      out.concat((long) value);
    } else {
      out.concatFloatRounded((float) value, 2);
    }
  }

  private void put(String name, Object getter, Watch watch) {
    if (name == null || getter == null) {
      return;
    }
    int index = indexOf(name);
    if (index >= 0) {
      watches.set(index, watch);
    } else {
      watches.add(watch);
    }
    markChanged();
  }

  private int indexOf(@Nullable String name) {
    if (name == null) {
      return -1;
    }
    for (int i = 0, n = watches.getSize(); i < n; i++) {
      if (watches.get(i).name.equals(name)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns how many watches exist.
   *
   * @return The watch count.
   */
  public int getCount() {
    return watches.getSize();
  }

  public boolean isMouseWatched() {
    return has(MOUSE_NAME);
  }

  public boolean isCameraWatched() {
    return has(CAMERA_NAME);
  }

  private abstract static class Watch {

    final String name;

    Watch(String name) {
      this.name = name;
    }

    abstract void writeValue(FlixelDebugMessageWriter writer, FlixelString scratch);
  }

  private static final class ObjectWatch extends Watch {

    private final Supplier<?> getter;

    ObjectWatch(String name, Supplier<?> getter) {
      super(name);
      this.getter = getter;
    }

    @Override
    void writeValue(FlixelDebugMessageWriter writer, FlixelString scratch) {
      Object value = getter.get();
      if (value instanceof Boolean b) {
        writer.writeValue(b.booleanValue());
      } else if (value instanceof Integer i) {
        writer.writeValue(i.intValue());
      } else if (value instanceof Long l) {
        writer.writeValue(l.longValue());
      } else if (value instanceof Float f) {
        writer.writeValue(f.floatValue());
      } else if (value instanceof Double d) {
        writer.writeValue(d.doubleValue());
      } else if (value instanceof Byte b) {
        writer.writeValue((int) b.byteValue());
      } else if (value instanceof Short s) {
        writer.writeValue((int) s.shortValue());
      } else if (value instanceof CharSequence text) {
        writer.writeValue(text);
      } else {
        scratch.clear();
        scratch.concat(value);
        writer.writeValue(scratch);
      }
    }
  }

  private static final class IntWatch extends Watch {

    private final IntSupplier getter;

    IntWatch(String name, IntSupplier getter) {
      super(name);
      this.getter = getter;
    }

    @Override
    void writeValue(FlixelDebugMessageWriter writer, FlixelString scratch) {
      writer.writeValue(getter.getAsInt());
    }
  }

  private static final class LongWatch extends Watch {

    private final LongSupplier getter;

    LongWatch(String name, LongSupplier getter) {
      super(name);
      this.getter = getter;
    }

    @Override
    void writeValue(FlixelDebugMessageWriter writer, FlixelString scratch) {
      writer.writeValue(getter.getAsLong());
    }
  }

  private static final class FloatWatch extends Watch {

    private final FloatSupplier getter;

    FloatWatch(String name, FloatSupplier getter) {
      super(name);
      this.getter = getter;
    }

    @Override
    void writeValue(FlixelDebugMessageWriter writer, FlixelString scratch) {
      writer.writeValue(getter.getAsFloat());
    }
  }

  private static final class DoubleWatch extends Watch {

    private final DoubleSupplier getter;

    DoubleWatch(String name, DoubleSupplier getter) {
      super(name);
      this.getter = getter;
    }

    @Override
    void writeValue(FlixelDebugMessageWriter writer, FlixelString scratch) {
      writer.writeValue(getter.getAsDouble());
    }
  }

  private static final class BooleanWatch extends Watch {

    private final BooleanSupplier getter;

    BooleanWatch(String name, BooleanSupplier getter) {
      super(name);
      this.getter = getter;
    }

    @Override
    void writeValue(FlixelDebugMessageWriter writer, FlixelString scratch) {
      writer.writeValue(getter.getAsBoolean());
    }
  }

  private static final class ByteWatch extends Watch {

    private final ByteSupplier getter;

    ByteWatch(String name, ByteSupplier getter) {
      super(name);
      this.getter = getter;
    }

    @Override
    void writeValue(FlixelDebugMessageWriter writer, FlixelString scratch) {
      writer.writeValue((int) getter.getAsByte());
    }
  }

  private static final class ShortWatch extends Watch {

    private final ShortSupplier getter;

    ShortWatch(String name, ShortSupplier getter) {
      super(name);
      this.getter = getter;
    }

    @Override
    void writeValue(FlixelDebugMessageWriter writer, FlixelString scratch) {
      writer.writeValue((int) getter.getAsShort());
    }
  }

  private static final class CharWatch extends Watch {

    private final CharSupplier getter;

    CharWatch(String name, CharSupplier getter) {
      super(name);
      this.getter = getter;
    }

    @Override
    void writeValue(FlixelDebugMessageWriter writer, FlixelString scratch) {
      scratch.clear();
      scratch.concat(getter.getAsChar());
      writer.writeValue(scratch);
    }
  }

  private static final class MouseWatch extends Watch {

    MouseWatch() {
      super(MOUSE_NAME);
    }

    @Override
    void writeValue(FlixelDebugMessageWriter writer, FlixelString scratch) {
      scratch.clear();
      if (Flixel.mouse != null) {
        scratch.concat(Flixel.mouse.getScreenX()).concat(", ").concat(Flixel.mouse.getScreenY());
      } else {
        scratch.concat(Flixel.input.getX()).concat(", ").concat(Flixel.input.getY());
      }
      writer.writeValue(scratch);
    }
  }

  private static final class CameraWatch extends Watch {

    CameraWatch() {
      super(CAMERA_NAME);
    }

    @Override
    void writeValue(FlixelDebugMessageWriter writer, FlixelString scratch) {
      scratch.clear();
      if (Flixel.cameras.getSize() == 0) {
        scratch.concat("none");
      } else {
        FlixelCamera cam = Flixel.cameras.get(0);
        scratch.concat("scroll ");
        appendNumber(scratch, cam.scrollX);
        scratch.concat(", ");
        appendNumber(scratch, cam.scrollY);
        scratch.concat(" zoom ");
        appendNumber(scratch, cam.getZoom());
      }
      writer.writeValue(scratch);
    }
  }
}
