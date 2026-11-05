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

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
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
 * Builds the rows and buttons of a custom debugger panel.
 *
 * <p>This is the same idea as {@link FlixelSectionBuilder}, minus the selected object: a custom panel is not about
 * one object, so its getters take no arguments and read whatever the game code captured in the lambda. Think of a
 * control panel on a machine: some labeled gauges (rows) and some buttons.
 *
 * <pre>{@code
 * Flixel.debug.register(new FlixelDebugCustomPanel("ai", "Enemy AI", p -> p
 *     .intProp("Active enemies", () -> enemies.countLiving())
 *     .floatProp("Aggression", () -> ai.aggression, v -> ai.aggression = v)
 *     .action("Kill all", enemies::killAll)));
 * }</pre>
 *
 * <p>As with the inspector, there is one method per value type so lambdas are never ambiguous, reading never
 * allocates, and a getter that throws shows its message as an error row.
 *
 * @see FlixelSectionBuilder
 */
public final class FlixelPanelBuilder {

  // The rows reuse the section class. Its type and title are never sent for a custom panel.
  private final FlixelDebugSection rows = new FlixelDebugSection(FlixelPanelBuilder.class, "Rows");
  private final FlixelArray<String> labels = new FlixelArray<>();
  private final FlixelArray<Runnable> actions = new FlixelArray<>();

  private FlixelDebugProperty<Object> last;

  private int epoch;

  /** Creates an empty builder. */
  public FlixelPanelBuilder() {}

  /**
   * Adds a read-only {@code boolean} row.
   *
   * @param name The label shown in the app.
   * @param getter Reads the value.
   * @return This builder.
   */
  @NotNull
  public FlixelPanelBuilder boolProp(@NotNull String name, @NotNull BooleanSupplier getter) {
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
  public FlixelPanelBuilder boolProp(@NotNull String name, @NotNull BooleanSupplier getter,
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
  public FlixelPanelBuilder intProp(@NotNull String name, @NotNull IntSupplier getter) {
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
  public FlixelPanelBuilder intProp(@NotNull String name, @NotNull IntSupplier getter, @NotNull IntConsumer setter) {
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
  public FlixelPanelBuilder longProp(@NotNull String name, @NotNull LongSupplier getter) {
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
  public FlixelPanelBuilder longProp(@NotNull String name, @NotNull LongSupplier getter,
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
  public FlixelPanelBuilder floatProp(@NotNull String name, @NotNull FloatSupplier getter) {
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
  public FlixelPanelBuilder floatProp(@NotNull String name, @NotNull FloatSupplier getter,
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
  public FlixelPanelBuilder doubleProp(@NotNull String name, @NotNull DoubleSupplier getter) {
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
  public FlixelPanelBuilder doubleProp(@NotNull String name, @NotNull DoubleSupplier getter,
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
  public FlixelPanelBuilder stringProp(@NotNull String name, @NotNull Consumer<FlixelString> getter) {
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
  public FlixelPanelBuilder stringProp(@NotNull String name, @NotNull Consumer<FlixelString> getter,
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
  public FlixelPanelBuilder enumProp(@NotNull String name,
      @NotNull Supplier<? extends Enum<?>> getter) {
    return add(FlixelDebugProperty.ofEnum(name, o -> getter.get()));
  }

  /**
   * Marks the row added last as read once instead of every refresh. The panel reads it again after
   * {@code FlixelDebugCustomPanel.refresh()} or after the row is edited.
   *
   * @return This builder.
   * @throws IllegalStateException If no row was added yet.
   */
  @NotNull
  public FlixelPanelBuilder once() {
    if (last == null) {
      throw new IllegalStateException("once() must come right after a row, for example intProp(...).once().");
    }
    last.markOnce();
    return this;
  }

  /**
   * Adds a button.
   *
   * @param label The text on the button.
   * @param onPress Runs on the game's main thread when the button is pressed in the app.
   * @return This builder.
   */
  @NotNull
  public FlixelPanelBuilder action(@NotNull String label, @NotNull Runnable onPress) {
    labels.add(label);
    actions.add(onPress);
    return this;
  }

  /**
   * Writes the panel payload: the rows, then the buttons.
   *
   * @param writer The writer positioned after the panel index.
   */
  public void write(@NotNull FlixelDebugMessageWriter writer) {
    rows.writeRows(writer, this, epoch);
    int n = Math.min(actions.getSize(), FlixelDebugProtocol.MAX_ARRAY_COUNT);
    writer.beginArray();
    for (int i = 0; i < n; i++) {
      writer.writeU16(i).writeString(labels.get(i));
    }
    writer.endArray(n);
  }

  /**
   * Applies a value sent by the app to a row.
   *
   * @param index The position of the row.
   * @param value The value from the app.
   * @return {@code true} when the row accepted the value.
   */
  public boolean setProperty(int index, @NotNull FlixelDebugValue value) {
    return rows.apply(index, this, value);
  }

  /**
   * Runs the action of a button.
   *
   * @param index The action index taken from the button.
   * @return {@code true} when a button with that index exists.
   */
  public boolean press(int index) {
    if (index < 0 || index >= actions.getSize()) {
      return false;
    }
    actions.get(index).run();
    return true;
  }

  /** Makes rows marked {@link #once()} read their getters again the next time the panel is written. */
  public void refresh() {
    epoch++;
  }

  private FlixelPanelBuilder add(FlixelDebugProperty<Object> property) {
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

  /**
   * Returns how many buttons were added.
   *
   * @return The button count.
   */
  public int getActionCount() {
    return actions.getSize();
  }
}
