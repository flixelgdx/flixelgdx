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
package org.flixelgdx.debug.inspector;

import org.flixelgdx.Flixel;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.functional.consumer.ObjBooleanConsumer;
import org.flixelgdx.functional.consumer.ObjFloatConsumer;
import org.flixelgdx.functional.function.StringAppender;
import org.flixelgdx.functional.function.ToBooleanFunction;
import org.flixelgdx.functional.function.ToFloatFunction;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * One named row of the debugger: a label, a value that is read from the game, and (optionally) a way to write a
 * new value back.
 *
 * <p>Think of a row on a car's dashboard. The label says "Speed", the needle shows the current reading, and some
 * rows (like the radio volume) also have a knob you can turn. A property is exactly that: a getter that reads the
 * needle and, when the row is editable, a setter that turns the knob.
 *
 * <p>You never create these directly. The builders {@link FlixelSectionBuilder} (for the inspector) and
 * {@link FlixelPropertyBuilder} (for property grids) create them through their {@code boolProp(...)},
 * {@code intProp(...)}, and similar methods.
 *
 * <h2>No garbage</h2>
 *
 * <p>Every property owns the small amount of state it needs to hold its latest reading (a number or a reused
 * {@link FlixelString}), so reading a value never boxes or allocates. If a getter throws, the row shows an
 * {@code ERROR} value with the exception message and the other rows keep working.
 *
 * <h2>Editing</h2>
 *
 * <p>When the debugger app sends a new value, the property converts it by kind. A numeric property accepts any
 * numeric kind (int, long, float, or double) and converts it the way a Java cast would, a boolean property accepts
 * only a boolean, and a text property accepts only text. A value of the wrong kind is ignored and a warning is
 * logged, and a getter-only property ignores every edit.
 *
 * @param <T> The type of the object the getters and setters work on.
 * @see FlixelSectionBuilder
 * @see FlixelPropertyBuilder
 */
public abstract class FlixelDebugProperty<T> {

  private static final String TAG = "Debugger/Inspector";

  private long longValue;
  private double doubleValue;

  private int kind;
  private int cachedEpoch;
  private final String name;
  private final FlixelString text = new FlixelString(32);

  private boolean editable;
  private boolean once;
  private boolean cached;

  /**
   * Creates a property.
   *
   * @param name The label shown in the app.
   */
  FlixelDebugProperty(@NotNull String name) {
    this.name = name;
  }

  /**
   * Creates a {@code boolean} property.
   *
   * @param name The label.
   * @param getter Reads the value.
   * @param setter Writes the value, or {@code null} for a read-only row.
   * @param <T> The object type.
   * @return The property.
   */
  static <T> FlixelDebugProperty<T> ofBool(String name, ToBooleanFunction<T> getter,
      @Nullable ObjBooleanConsumer<T> setter) {
    return new BoolProperty<>(name, getter, setter);
  }

  /**
   * Creates an {@code int} property.
   *
   * @param name The label.
   * @param getter Reads the value.
   * @param setter Writes the value, or {@code null} for a read-only row.
   * @param <T> The object type.
   * @return The property.
   */
  static <T> FlixelDebugProperty<T> ofInt(String name, ToIntFunction<T> getter, @Nullable ObjIntConsumer<T> setter) {
    return new IntProperty<>(name, getter, setter);
  }

  /**
   * Creates a {@code long} property.
   *
   * @param name The label.
   * @param getter Reads the value.
   * @param setter Writes the value, or {@code null} for a read-only row.
   * @param <T> The object type.
   * @return The property.
   */
  static <T> FlixelDebugProperty<T> ofLong(String name, ToLongFunction<T> getter, @Nullable ObjLongConsumer<T> setter) {
    return new LongProperty<>(name, getter, setter);
  }

  /**
   * Creates a {@code float} property.
   *
   * @param name The label.
   * @param getter Reads the value.
   * @param setter Writes the value, or {@code null} for a read-only row.
   * @param <T> The object type.
   * @return The property.
   */
  static <T> FlixelDebugProperty<T> ofFloat(String name, ToFloatFunction<T> getter,
      @Nullable ObjFloatConsumer<T> setter) {
    return new FloatProperty<>(name, getter, setter);
  }

  /**
   * Creates a {@code double} property.
   *
   * @param name The label.
   * @param getter Reads the value.
   * @param setter Writes the value, or {@code null} for a read-only row.
   * @param <T> The object type.
   * @return The property.
   */
  static <T> FlixelDebugProperty<T> ofDouble(String name, ToDoubleFunction<T> getter,
      @Nullable ObjDoubleConsumer<T> setter) {
    return new DoubleProperty<>(name, getter, setter);
  }

  /**
   * Creates a text property.
   *
   * @param name The label.
   * @param getter Appends the text into a reused buffer.
   * @param setter Receives the new text (a reused buffer, so copy it to keep it), or {@code null} for a
   *     read-only row.
   * @param <T> The object type.
   * @return The property.
   */
  static <T> FlixelDebugProperty<T> ofString(String name, StringAppender<T> getter,
      @Nullable BiConsumer<T, CharSequence> setter) {
    return new StringProperty<>(name, getter, setter);
  }

  /**
   * Creates a read-only property that shows the name of an enum constant.
   *
   * @param name The label.
   * @param getter Reads the constant. A {@code null} constant shows as {@code "null"}.
   * @param <T> The object type.
   * @return The property.
   */
  static <T> FlixelDebugProperty<T> ofEnum(String name, Function<T, ? extends Enum<?>> getter) {
    return new EnumProperty<>(name, getter);
  }

  /**
   * Writes this row (name, value, editable flag) into a message.
   *
   * @param writer The writer positioned where a property row starts.
   * @param target The object to read from.
   * @param epoch The current read epoch. A property marked {@link #markOnce() once} reads its getter again only
   *     when the epoch differs from the one it last read in.
   */
  public final void writeRow(@NotNull FlixelDebugMessageWriter writer, T target, int epoch) {
    refresh(target, epoch);
    writer.writeString(name);
    writeValue(writer);
    writer.writeBool(editable);
  }

  /**
   * Writes this row as a {@code LIST [STRING name, value, BOOL editable]} value, the form a property grid widget
   * uses.
   *
   * @param writer The writer.
   * @param target The object to read from.
   * @param epoch The current read epoch, as for {@link #writeRow(FlixelDebugMessageWriter, Object, int)}.
   */
  public final void writeListRow(@NotNull FlixelDebugMessageWriter writer, T target, int epoch) {
    refresh(target, epoch);
    writer.beginList().writeValue(name);
    writeValue(writer);
    writer.writeValue(editable).endList(3);
  }

  /**
   * Applies a value sent by the app to the target.
   *
   * @param target The object to write to.
   * @param value The value from the app.
   * @return {@code true} when the setter ran. {@code false} when the row is read-only, the kind did not match,
   *     or the setter threw (a warning is logged for the last two).
   */
  public final boolean apply(T target, @NotNull FlixelDebugValue value) {
    if (!editable) {
      return false;
    }
    boolean applied;
    try {
      applied = write(target, value);
    } catch (Exception e) {
      Flixel.log.tagged(TAG).warn("Setting the property '{}' failed: {}", name, e.getMessage());
      return false;
    }
    if (!applied) {
      Flixel.log.tagged(TAG).warn("Ignored a value of kind {} for the property '{}'.", value.getKind(), name);
      return false;
    }
    cached = false;
    return true;
  }

  /**
   * Marks this row as read once per epoch instead of every time the panel is written.
   *
   * <p>Use it for values that cannot change while an object is selected, such as an id or a type name.
   */
  final void markOnce() {
    once = true;
  }

  /**
   * Reads the current value from the target into this property's own state with one of the {@code put*} methods.
   *
   * @param target The object to read from.
   */
  abstract void read(T target);

  /**
   * Converts the value by kind and passes it to the setter.
   *
   * @param target The object to write to.
   * @param value The value from the app.
   * @return {@code false} when the kind does not fit this property.
   */
  abstract boolean write(T target, FlixelDebugValue value);

  final void putBool(boolean value) {
    kind = FlixelDebugProtocol.VALUE_BOOL;
    longValue = value ? 1L : 0L;
  }

  final void putInt(int value) {
    kind = FlixelDebugProtocol.VALUE_INT;
    longValue = value;
  }

  final void putLong(long value) {
    kind = FlixelDebugProtocol.VALUE_LONG;
    longValue = value;
  }

  final void putFloat(float value) {
    kind = FlixelDebugProtocol.VALUE_FLOAT;
    doubleValue = value;
  }

  final void putDouble(double value) {
    kind = FlixelDebugProtocol.VALUE_DOUBLE;
    doubleValue = value;
  }

  /**
   * Starts a text reading and returns the cleared buffer to fill.
   *
   * @return The reused buffer.
   */
  final FlixelString beginText() {
    kind = FlixelDebugProtocol.VALUE_STRING;
    text.clear();
    return text;
  }

  final void putError(CharSequence message) {
    kind = FlixelDebugProtocol.VALUE_ERROR;
    text.clear();
    text.concat(message);
  }

  private void refresh(T target, int epoch) {
    if (!once || !cached || cachedEpoch != epoch) {
      try {
        read(target);
      } catch (Exception e) {
        String message = e.getMessage();
        putError(message != null ? message : e.getClass().getName());
      }
      cached = true;
      cachedEpoch = epoch;
    }
  }

  private void writeValue(FlixelDebugMessageWriter writer) {
    switch (kind) {
      case FlixelDebugProtocol.VALUE_BOOL -> writer.writeValue(longValue != 0L);
      case FlixelDebugProtocol.VALUE_INT -> writer.writeValue((int) longValue);
      case FlixelDebugProtocol.VALUE_LONG -> writer.writeValue(longValue);
      case FlixelDebugProtocol.VALUE_FLOAT -> writer.writeValue((float) doubleValue);
      case FlixelDebugProtocol.VALUE_DOUBLE -> writer.writeValue(doubleValue);
      case FlixelDebugProtocol.VALUE_ERROR -> writer.writeErrorValue(text);
      default -> writer.writeValue(text);
    }
  }

  /**
   * Returns the label shown in the app.
   *
   * @return The name.
   */
  @NotNull
  public final String getName() {
    return name;
  }

  /**
   * Returns whether the app may edit this row.
   *
   * @return {@code true} when a setter was given.
   */
  public final boolean isEditable() {
    return editable;
  }

  /**
   * Returns whether this row is read only once per epoch.
   *
   * @return {@code true} for a row marked once.
   */
  public final boolean isOnce() {
    return once;
  }

  final void setEditable(boolean editable) {
    this.editable = editable;
  }

  private static final class BoolProperty<T> extends FlixelDebugProperty<T> {

    private final ToBooleanFunction<T> getter;
    private final ObjBooleanConsumer<T> setter;

    BoolProperty(String name, ToBooleanFunction<T> getter, ObjBooleanConsumer<T> setter) {
      super(name);
      this.getter = getter;
      this.setter = setter;
      setEditable(setter != null);
    }

    @Override
    void read(T target) {
      putBool(getter.applyAsBoolean(target));
    }

    @Override
    boolean write(T target, FlixelDebugValue value) {
      if (value.getKind() != FlixelDebugProtocol.VALUE_BOOL) {
        return false;
      }
      setter.accept(target, value.asBoolean());
      return true;
    }
  }

  private static final class IntProperty<T> extends FlixelDebugProperty<T> {

    private final ToIntFunction<T> getter;
    private final ObjIntConsumer<T> setter;

    IntProperty(String name, ToIntFunction<T> getter, ObjIntConsumer<T> setter) {
      super(name);
      this.getter = getter;
      this.setter = setter;
      setEditable(setter != null);
    }

    @Override
    void read(T target) {
      putInt(getter.applyAsInt(target));
    }

    @Override
    boolean write(T target, FlixelDebugValue value) {
      if (!value.isNumber()) {
        return false;
      }
      setter.accept(target, value.asInt());
      return true;
    }
  }

  private static final class LongProperty<T> extends FlixelDebugProperty<T> {

    private final ToLongFunction<T> getter;
    private final ObjLongConsumer<T> setter;

    LongProperty(String name, ToLongFunction<T> getter, ObjLongConsumer<T> setter) {
      super(name);
      this.getter = getter;
      this.setter = setter;
      setEditable(setter != null);
    }

    @Override
    void read(T target) {
      putLong(getter.applyAsLong(target));
    }

    @Override
    boolean write(T target, FlixelDebugValue value) {
      if (!value.isNumber()) {
        return false;
      }
      setter.accept(target, value.asLong());
      return true;
    }
  }

  private static final class FloatProperty<T> extends FlixelDebugProperty<T> {

    private final ToFloatFunction<T> getter;
    private final ObjFloatConsumer<T> setter;

    FloatProperty(String name, ToFloatFunction<T> getter, ObjFloatConsumer<T> setter) {
      super(name);
      this.getter = getter;
      this.setter = setter;
      setEditable(setter != null);
    }

    @Override
    void read(T target) {
      putFloat(getter.applyAsFloat(target));
    }

    @Override
    boolean write(T target, FlixelDebugValue value) {
      if (!value.isNumber()) {
        return false;
      }
      setter.accept(target, value.asFloat());
      return true;
    }
  }

  private static final class DoubleProperty<T> extends FlixelDebugProperty<T> {

    private final ToDoubleFunction<T> getter;
    private final ObjDoubleConsumer<T> setter;

    DoubleProperty(String name, ToDoubleFunction<T> getter, ObjDoubleConsumer<T> setter) {
      super(name);
      this.getter = getter;
      this.setter = setter;
      setEditable(setter != null);
    }

    @Override
    void read(T target) {
      putDouble(getter.applyAsDouble(target));
    }

    @Override
    boolean write(T target, FlixelDebugValue value) {
      if (!value.isNumber()) {
        return false;
      }
      setter.accept(target, value.asDouble());
      return true;
    }
  }

  private static final class StringProperty<T> extends FlixelDebugProperty<T> {

    private final StringAppender<T> getter;
    private final BiConsumer<T, CharSequence> setter;

    StringProperty(String name, StringAppender<T> getter, BiConsumer<T, CharSequence> setter) {
      super(name);
      this.getter = getter;
      this.setter = setter;
      setEditable(setter != null);
    }

    @Override
    void read(T target) {
      getter.appendTo(target, beginText());
    }

    @Override
    boolean write(T target, FlixelDebugValue value) {
      if (value.getKind() != FlixelDebugProtocol.VALUE_STRING) {
        return false;
      }
      setter.accept(target, value.getText());
      return true;
    }
  }

  private static final class EnumProperty<T> extends FlixelDebugProperty<T> {

    private final Function<T, ? extends Enum<?>> getter;

    EnumProperty(String name, Function<T, ? extends Enum<?>> getter) {
      super(name);
      this.getter = getter;
    }

    @Override
    void read(T target) {
      Enum<?> constant = getter.apply(target);
      beginText().concat(constant != null ? constant.name() : "null");
    }

    @Override
    boolean write(T target, FlixelDebugValue value) {
      return false;
    }
  }
}
