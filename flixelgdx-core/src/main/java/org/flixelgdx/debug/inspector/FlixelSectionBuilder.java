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

import org.flixelgdx.functional.consumer.ObjBooleanConsumer;
import org.flixelgdx.functional.consumer.ObjFloatConsumer;
import org.flixelgdx.functional.function.StringAppender;
import org.flixelgdx.functional.function.ToBooleanFunction;
import org.flixelgdx.functional.function.ToFloatFunction;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * Builds one inspector section: the list of rows the debugger shows for a type of object.
 *
 * <p>Think of filling in a character sheet. For every stat you write its label, tell the sheet how to read the
 * number from your character, and (if the stat can be changed) how to write a new number back. Every method adds
 * one row and returns the builder, so rows are chained in the order they appear.
 *
 * <p>There is one method per value type ({@link #boolProp}, {@link #intProp}, {@link #longProp},
 * {@link #floatProp}, {@link #doubleProp}, and {@link #stringProp}) instead of one overloaded method, so a
 * lambda always means exactly one thing, including in Kotlin. Giving a setter makes the row editable.
 *
 * <p>Example, shown for a {@code Player} that is selected in the debugger:
 *
 * <pre>{@code
 * Flixel.debug.inspector.register(Player.class, "Player", b -> b
 *     .intProp("Health", Player::getHealth, Player::setHealth)
 *     .floatProp("Speed", Player::getSpeed)
 *     .boolProp("On ground", Player::isOnGround)
 *     .stringProp("Name", (p, out) -> out.concat(p.getName()))
 *     .enumProp("State", Player::getState)
 *     .intProp("Id", Player::getId).once());
 * }</pre>
 *
 * <p>Reading a value never boxes or allocates. If a getter throws, the row shows the exception message instead of
 * a value.
 *
 * @param <T> The type of object the getters and setters work on.
 * @see FlixelDebugSection
 */
public final class FlixelSectionBuilder<T> {

  private final FlixelDebugSection section;

  private FlixelDebugProperty<T> last;

  /**
   * Creates a builder that adds to the given section.
   *
   * @param section The section to fill.
   */
  public FlixelSectionBuilder(@NotNull FlixelDebugSection section) {
    this.section = section;
  }

  /**
   * Adds a read-only {@code boolean} row.
   *
   * @param name The label shown in the app.
   * @param getter Reads the value.
   * @return This builder.
   */
  @NotNull
  public FlixelSectionBuilder<T> boolProp(@NotNull String name, @NotNull ToBooleanFunction<T> getter) {
    return add(FlixelDebugProperty.ofBool(name, getter, null));
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
  public FlixelSectionBuilder<T> boolProp(@NotNull String name, @NotNull ToBooleanFunction<T> getter,
      @NotNull ObjBooleanConsumer<T> setter) {
    return add(FlixelDebugProperty.ofBool(name, getter, setter));
  }

  /**
   * Adds a read-only {@code int} row.
   *
   * @param name The label shown in the app.
   * @param getter Reads the value.
   * @return This builder.
   */
  @NotNull
  public FlixelSectionBuilder<T> intProp(@NotNull String name, @NotNull ToIntFunction<T> getter) {
    return add(FlixelDebugProperty.ofInt(name, getter, null));
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
  public FlixelSectionBuilder<T> intProp(@NotNull String name, @NotNull ToIntFunction<T> getter,
      @NotNull ObjIntConsumer<T> setter) {
    return add(FlixelDebugProperty.ofInt(name, getter, setter));
  }

  /**
   * Adds a read-only {@code long} row.
   *
   * @param name The label shown in the app.
   * @param getter Reads the value.
   * @return This builder.
   */
  @NotNull
  public FlixelSectionBuilder<T> longProp(@NotNull String name, @NotNull ToLongFunction<T> getter) {
    return add(FlixelDebugProperty.ofLong(name, getter, null));
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
  public FlixelSectionBuilder<T> longProp(@NotNull String name, @NotNull ToLongFunction<T> getter,
      @NotNull ObjLongConsumer<T> setter) {
    return add(FlixelDebugProperty.ofLong(name, getter, setter));
  }

  /**
   * Adds a read-only {@code float} row.
   *
   * @param name The label shown in the app.
   * @param getter Reads the value.
   * @return This builder.
   */
  @NotNull
  public FlixelSectionBuilder<T> floatProp(@NotNull String name, @NotNull ToFloatFunction<T> getter) {
    return add(FlixelDebugProperty.ofFloat(name, getter, null));
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
  public FlixelSectionBuilder<T> floatProp(@NotNull String name, @NotNull ToFloatFunction<T> getter,
      @NotNull ObjFloatConsumer<T> setter) {
    return add(FlixelDebugProperty.ofFloat(name, getter, setter));
  }

  /**
   * Adds a read-only {@code double} row.
   *
   * @param name The label shown in the app.
   * @param getter Reads the value.
   * @return This builder.
   */
  @NotNull
  public FlixelSectionBuilder<T> doubleProp(@NotNull String name, @NotNull ToDoubleFunction<T> getter) {
    return add(FlixelDebugProperty.ofDouble(name, getter, null));
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
  public FlixelSectionBuilder<T> doubleProp(@NotNull String name, @NotNull ToDoubleFunction<T> getter,
      @NotNull ObjDoubleConsumer<T> setter) {
    return add(FlixelDebugProperty.ofDouble(name, getter, setter));
  }

  /**
   * Adds a read-only text row.
   *
   * @param name The label shown in the app.
   * @param getter Appends the text into the reused buffer it is given, for example
   *     {@code (p, out) -> out.concat(p.getName())}.
   * @return This builder.
   */
  @NotNull
  public FlixelSectionBuilder<T> stringProp(@NotNull String name, @NotNull StringAppender<T> getter) {
    return add(FlixelDebugProperty.ofString(name, getter, null));
  }

  /**
   * Adds an editable text row.
   *
   * @param name The label shown in the app.
   * @param getter Appends the text into the reused buffer it is given.
   * @param setter Receives the new text. The text is a reused buffer that is only valid during the call, so
   *     call {@code toString()} on it if you need to keep it.
   * @return This builder.
   */
  @NotNull
  public FlixelSectionBuilder<T> stringProp(@NotNull String name, @NotNull StringAppender<T> getter,
      @NotNull BiConsumer<T, CharSequence> setter) {
    return add(FlixelDebugProperty.ofString(name, getter, setter));
  }

  /**
   * Adds a read-only row that shows the name of an enum constant, sent as text.
   *
   * @param name The label shown in the app.
   * @param getter Reads the constant. {@code null} shows as {@code "null"}.
   * @return This builder.
   */
  @NotNull
  public FlixelSectionBuilder<T> enumProp(@NotNull String name, @NotNull Function<T, ? extends Enum<?>> getter) {
    return add(FlixelDebugProperty.ofEnum(name, getter));
  }

  /**
   * Marks the row added last as read once per selection instead of every refresh.
   *
   * <p>Use it for values that cannot change while an object is selected, such as an id. The value is read when the
   * object is selected and again only if it is selected again or the row is edited.
   *
   * @return This builder.
   * @throws IllegalStateException If no row was added yet.
   */
  @NotNull
  public FlixelSectionBuilder<T> once() {
    if (last == null) {
      throw new IllegalStateException("once() must come right after a row, for example intProp(...).once().");
    }
    last.markOnce();
    return this;
  }

  /**
   * Makes this section read from a related object instead of the selected one.
   *
   * <p>Think of a "Weapon" card on a character sheet: the card is shown when a player is selected, but its rows
   * read from the player's weapon. The function receives the selected object (an instance of the class this
   * section is registered for) and returns the object the rows should read from. When it returns {@code null}, the
   * whole section is hidden for that selection.
   *
   * <p>The returned builder works on the related object, so call {@code find(...)} <em>first</em>, before adding
   * rows:
   *
   * <pre>{@code
   * Flixel.debug.inspector.register(Player.class, "Weapon", b -> b
   *     .find(Player::getWeapon)
   *     .intProp("Ammo", Weapon::getAmmo, Weapon::setAmmo));
   * }</pre>
   *
   * @param target Maps the selected object to the object the rows read from.
   * @param <R> The type of the related object.
   * @return A builder for the related object that adds to the same section.
   * @throws IllegalStateException If rows were already added, since they were written for the selected object.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public <R> FlixelSectionBuilder<R> find(@NotNull Function<? super T, ? extends R> target) {
    if (section.getCount() > 0) {
      throw new IllegalStateException("find(...) must be called before any row is added.");
    }
    section.chainFinder(o -> target.apply((T) o));
    return new FlixelSectionBuilder<>(section);
  }

  private FlixelSectionBuilder<T> add(FlixelDebugProperty<T> property) {
    section.add(property);
    last = property;
    return this;
  }
}
