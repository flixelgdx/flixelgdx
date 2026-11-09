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

import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.ui.FlixelDebugProperties;
import org.flixelgdx.functional.consumer.BooleanConsumer;
import org.flixelgdx.functional.consumer.FloatConsumer;
import org.flixelgdx.functional.supplier.FloatSupplier;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Builds the live rows of one section of a property grid ({@link FlixelDebugProperties}) from getters and setters.
 *
 * <p>This is the same idea as {@link FlixelSectionBuilder}, minus the selected object: a section of a panel is not
 * about one object, so its getters take no arguments and read whatever the game code captured in the lambda. Think of
 * a dashboard on a machine: a few labeled gauges, some of which have a knob to turn (a setter).
 *
 * <pre>{@code
 * FlixelDebugProperties ai = new FlixelDebugProperties().section("Enemy AI", s -> s
 *     .intProp("Active enemies", () -> enemies.countLiving())
 *     .floatProp("Aggression", () -> ai.aggression, v -> ai.aggression = v)
 *     .boolProp("Paused", () -> ai.paused, v -> ai.paused = v));
 * }</pre>
 *
 * <p>As with the inspector, there is one method per value type so lambdas are never ambiguous, reading never
 * allocates, and a getter that throws shows its message as an error row. A row with a setter can be edited in the
 * app; a row without one is read-only.
 *
 * @see FlixelSectionBuilder
 * @see FlixelDebugProperties
 */
public final class FlixelPropertyBuilder {

  // The rows reuse the section class. Its type and title are never sent; the property grid sends its own title.
  private final FlixelDebugSection rows = new FlixelDebugSection(FlixelPropertyBuilder.class, "Rows");

  private FlixelDebugProperty<Object> last;

  private int epoch;

  /** Creates an empty builder. */
  public FlixelPropertyBuilder() {}

  /**
   * Adds a read-only {@code boolean} row.
   *
   * @param name The label shown in the app.
   * @param getter Reads the value.
   * @return This builder.
   */
  @NotNull
  public FlixelPropertyBuilder boolProp(@NotNull String name, @NotNull BooleanSupplier getter) {
    return add(FlixelDebugProperty.ofBool(name, o -> getter.getAsBoolean(), null));
  }

  /**
   * Adds an editable {@code boolean} row.
   *
   * @param name The label shown in the app.
   * @param getter Reads the value.
   * @param setter Writes a new value.
   * @return This builder.
   */
  @NotNull
  public FlixelPropertyBuilder boolProp(@NotNull String name, @NotNull BooleanSupplier getter,
      @NotNull BooleanConsumer setter) {
    return add(FlixelDebugProperty.ofBool(name, o -> getter.getAsBoolean(), (o, v) -> setter.accept(v)));
  }

  /**
   * Adds a read-only {@code int} row.
   *
   * @param name The label shown in the app.
   * @param getter Reads the value.
   * @return This builder.
   */
  @NotNull
  public FlixelPropertyBuilder intProp(@NotNull String name, @NotNull IntSupplier getter) {
    return add(FlixelDebugProperty.ofInt(name, o -> getter.getAsInt(), null));
  }

  /**
   * Adds an editable {@code int} row.
   *
   * @param name The label shown in the app.
   * @param getter Reads the value.
   * @param setter Writes a new value.
   * @return This builder.
   */
  @NotNull
  public FlixelPropertyBuilder intProp(@NotNull String name, @NotNull IntSupplier getter, @NotNull IntConsumer setter) {
    return add(FlixelDebugProperty.ofInt(name, o -> getter.getAsInt(), (o, v) -> setter.accept(v)));
  }

  /**
   * Adds a read-only {@code long} row.
   *
   * @param name The label shown in the app.
   * @param getter Reads the value.
   * @return This builder.
   */
  @NotNull
  public FlixelPropertyBuilder longProp(@NotNull String name, @NotNull LongSupplier getter) {
    return add(FlixelDebugProperty.ofLong(name, o -> getter.getAsLong(), null));
  }

  /**
   * Adds an editable {@code long} row.
   *
   * @param name The label shown in the app.
   * @param getter Reads the value.
   * @param setter Writes a new value.
   * @return This builder.
   */
  @NotNull
  public FlixelPropertyBuilder longProp(@NotNull String name, @NotNull LongSupplier getter,
      @NotNull LongConsumer setter) {
    return add(FlixelDebugProperty.ofLong(name, o -> getter.getAsLong(), (o, v) -> setter.accept(v)));
  }

  /**
   * Adds a read-only {@code float} row.
   *
   * @param name The label shown in the app.
   * @param getter Reads the value.
   * @return This builder.
   */
  @NotNull
  public FlixelPropertyBuilder floatProp(@NotNull String name, @NotNull FloatSupplier getter) {
    return add(FlixelDebugProperty.ofFloat(name, o -> getter.getAsFloat(), null));
  }

  /**
   * Adds an editable {@code float} row.
   *
   * @param name The label shown in the app.
   * @param getter Reads the value.
   * @param setter Writes a new value.
   * @return This builder.
   */
  @NotNull
  public FlixelPropertyBuilder floatProp(@NotNull String name, @NotNull FloatSupplier getter,
      @NotNull FloatConsumer setter) {
    return add(FlixelDebugProperty.ofFloat(name, o -> getter.getAsFloat(), (o, v) -> setter.accept(v)));
  }

  /**
   * Adds a read-only {@code double} row.
   *
   * @param name The label shown in the app.
   * @param getter Reads the value.
   * @return This builder.
   */
  @NotNull
  public FlixelPropertyBuilder doubleProp(@NotNull String name, @NotNull DoubleSupplier getter) {
    return add(FlixelDebugProperty.ofDouble(name, o -> getter.getAsDouble(), null));
  }

  /**
   * Adds an editable {@code double} row.
   *
   * @param name The label shown in the app.
   * @param getter Reads the value.
   * @param setter Writes a new value.
   * @return This builder.
   */
  @NotNull
  public FlixelPropertyBuilder doubleProp(@NotNull String name, @NotNull DoubleSupplier getter,
      @NotNull DoubleConsumer setter) {
    return add(FlixelDebugProperty.ofDouble(name, o -> getter.getAsDouble(), (o, v) -> setter.accept(v)));
  }

  /**
   * Adds a read-only text row.
   *
   * @param name The label shown in the app.
   * @param getter Appends the text into the reused buffer it is given, for example
   *     {@code out -> out.concat(boss.getName())}.
   * @return This builder.
   */
  @NotNull
  public FlixelPropertyBuilder stringProp(@NotNull String name, @NotNull Consumer<FlixelString> getter) {
    return add(FlixelDebugProperty.ofString(name, (o, out) -> getter.accept(out), null));
  }

  /**
   * Adds an editable text row.
   *
   * @param name The label shown in the app.
   * @param getter Appends the text into the reused buffer it is given.
   * @param setter Receives the new text. The text is a reused buffer that is only valid during the call, so call
   *     {@code toString()} on it if you need to keep it.
   * @return This builder.
   */
  @NotNull
  public FlixelPropertyBuilder stringProp(@NotNull String name, @NotNull Consumer<FlixelString> getter,
      @NotNull Consumer<CharSequence> setter) {
    return add(FlixelDebugProperty.ofString(name, (o, out) -> getter.accept(out), (o, v) -> setter.accept(v)));
  }

  /**
   * Adds a read-only row that shows the name of an enum constant, sent as text.
   *
   * @param name The label shown in the app.
   * @param getter Reads the constant. {@code null} shows as {@code "null"}.
   * @return This builder.
   */
  @NotNull
  public FlixelPropertyBuilder enumProp(@NotNull String name,
      @NotNull Supplier<? extends Enum<?>> getter) {
    return add(FlixelDebugProperty.ofEnum(name, o -> getter.get()));
  }

  /**
   * Marks the row added last as read once instead of every refresh. The row reads its getter again after
   * {@link #refresh()} or after the row is edited.
   *
   * @return This builder.
   * @throws IllegalStateException If no row was added yet.
   */
  @NotNull
  public FlixelPropertyBuilder once() {
    if (last == null) {
      throw new IllegalStateException("once() must come right after a row, for example intProp(...).once().");
    }
    last.markOnce();
    return this;
  }

  /**
   * Writes every row as a {@code LIST [STRING name, value, BOOL editable]} item, reading the getters.
   *
   * @param writer The writer, positioned inside the rows list of a section.
   * @return How many rows were written.
   */
  public int writeItems(@NotNull FlixelDebugMessageWriter writer) {
    return rows.writeListItems(writer, this, epoch);
  }

  /**
   * Applies a value sent by the app to a row.
   *
   * @param index The position of the row.
   * @param value The value from the app.
   * @return {@code true} when the row accepted the value.
   */
  public boolean apply(int index, @NotNull FlixelDebugValue value) {
    return rows.apply(index, this, value);
  }

  /** Makes rows marked {@link #once()} read their getters again the next time the rows are written. */
  public void refresh() {
    epoch++;
  }

  private FlixelPropertyBuilder add(FlixelDebugProperty<Object> property) {
    rows.add(property);
    last = property;
    return this;
  }

  /**
   * Returns how many rows were added.
   *
   * @return The row count.
   */
  public int getRowCount() {
    return rows.getCount();
  }
}
