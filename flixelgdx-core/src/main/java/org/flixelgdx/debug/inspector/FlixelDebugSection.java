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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * A titled group of {@linkplain FlixelDebugProperty properties} that belongs to one type of object.
 *
 * <p>Think of a section as one card in a character sheet: "Movement", "Appearance", "Animation". The inspector
 * shows one card for every registered class in the selected object's family tree, and each card lists its own
 * rows.
 *
 * <p>A section has a type (the class it was registered for), a display name, an ordered list of properties, and an
 * optional <em>finder</em>. When a finder is present, the section reads from the object the finder returns instead
 * of the selected object, and it is hidden when the finder returns {@code null}.
 *
 * <p>Sections are built with {@link FlixelSectionBuilder} and registered through
 * {@code Flixel.debug.inspector.register(...)}.
 *
 * @see FlixelSectionBuilder
 */
public final class FlixelDebugSection {

  private final FlixelArray<FlixelDebugProperty<Object>> properties = new FlixelArray<>();

  private final Class<?> type;
  private final String typeName;
  private final String displayName;

  @Nullable
  private Function<Object, Object> finder;

  /**
   * Creates an empty section.
   *
   * @param type The class this section is registered for.
   * @param displayName The title shown in the app.
   */
  public FlixelDebugSection(@NotNull Class<?> type, @NotNull String displayName) {
    this.type = type;
    this.displayName = displayName;
    String simple = type.getSimpleName();
    this.typeName = simple.isEmpty() ? type.getName() : simple;
  }

  /**
   * Finds the object this section reads from.
   *
   * @param selected The selected object.
   * @return The selected object itself when the section has no finder, otherwise whatever the finder returns.
   *     {@code null} means the section is hidden. A finder that throws also hides the section.
   */
  @Nullable
  public Object resolve(@NotNull Object selected) {
    if (finder == null) {
      return selected;
    }
    try {
      return finder.apply(selected);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Writes the rows of this section: a {@code u16}-counted array of property rows.
   *
   * @param writer The writer positioned where the rows start.
   * @param target The object to read from, as returned by {@link #resolve(Object)}.
   * @param epoch The read epoch, see {@link FlixelDebugProperty#writeRow(FlixelDebugMessageWriter, Object, int)}.
   */
  public void writeRows(@NotNull FlixelDebugMessageWriter writer, @NotNull Object target, int epoch) {
    int n = Math.min(properties.getSize(), FlixelDebugProtocol.MAX_ARRAY_COUNT);
    writer.beginArray();
    for (int i = 0; i < n; i++) {
      properties.get(i).writeRow(writer, target, epoch);
    }
    writer.endArray(n);
  }

  /**
   * Writes the rows of this section as items of a list, each a {@code LIST [STRING name, value, BOOL editable]}, the
   * form a property grid widget uses.
   *
   * @param writer The writer, positioned inside a list.
   * @param target The object to read from, as returned by {@link #resolve(Object)}.
   * @param epoch The read epoch, see {@link FlixelDebugProperty#writeRow(FlixelDebugMessageWriter, Object, int)}.
   * @return How many rows were written.
   */
  public int writeListItems(@NotNull FlixelDebugMessageWriter writer, @NotNull Object target, int epoch) {
    int n = Math.min(properties.getSize(), FlixelDebugProtocol.MAX_ARRAY_COUNT);
    for (int i = 0; i < n; i++) {
      properties.get(i).writeListRow(writer, target, epoch);
    }
    return n;
  }

  /**
   * Applies a value sent by the app to one property.
   *
   * @param index The position of the property, as sent in the rows.
   * @param target The object to write to, as returned by {@link #resolve(Object)}.
   * @param value The value from the app.
   * @return {@code true} when the property accepted the value.
   */
  public boolean apply(int index, @NotNull Object target, @NotNull FlixelDebugValue value) {
    if (index < 0 || index >= properties.getSize()) {
      return false;
    }
    return properties.get(index).apply(target, value);
  }

  /**
   * Appends a property to this section.
   *
   * @param property The property to add.
   * @param <T> The object type the property works on.
   */
  @SuppressWarnings("unchecked")
  <T> void add(@NotNull FlixelDebugProperty<T> property) {
    properties.add((FlixelDebugProperty<Object>) property);
  }

  /**
   * Chains a finder after the current one, so the result of the existing finder (or the selection when there is
   * none) is passed to the new one.
   *
   * @param next The next step.
   */
  void chainFinder(@NotNull Function<Object, Object> next) {
    Function<Object, Object> current = finder;
    if (current == null) {
      finder = next;
    } else {
      finder = o -> {
        Object mid = current.apply(o);
        return mid == null ? null : next.apply(mid);
      };
    }
  }

  /**
   * Returns the properties in the order they are sent.
   *
   * @param index A position from 0 to {@link #getCount()} minus 1.
   * @return The property.
   */
  @NotNull
  public FlixelDebugProperty<Object> getProperty(int index) {
    return properties.get(index);
  }

  /**
   * Returns how many properties this section has.
   *
   * @return The property count.
   */
  public int getCount() {
    return properties.getSize();
  }

  /**
   * Returns the class this section was registered for.
   *
   * @return The class.
   */
  @NotNull
  public Class<?> getType() {
    return type;
  }

  /**
   * Returns the simple name of the registered class, as sent in the {@code typeName} field.
   *
   * @return The type name.
   */
  @NotNull
  public String getTypeName() {
    return typeName;
  }

  /**
   * Returns the title shown in the app.
   *
   * @return The display name.
   */
  @NotNull
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Returns whether this section has a finder.
   *
   * @return {@code true} when {@link FlixelSectionBuilder#find(Function)} was used.
   */
  public boolean hasFinder() {
    return finder != null;
  }
}
