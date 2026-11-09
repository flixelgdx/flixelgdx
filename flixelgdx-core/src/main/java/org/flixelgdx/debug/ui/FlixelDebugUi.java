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
package org.flixelgdx.debug.ui;

import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.functional.consumer.BooleanConsumer;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Short factory methods for building debugger panels, so a whole panel reads like an outline of what it shows.
 *
 * <p>Every method simply creates a widget with the given settings; there is nothing here that the widget classes
 * cannot do on their own. Import the methods statically and a panel becomes compact:
 *
 * <pre>{@code
 * import static org.flixelgdx.debug.ui.FlixelDebugUi.*;
 *
 * panel.setContent(column(
 *     heading("Player"),
 *     row(button("Heal", player::heal), button("Kill", player::kill)),
 *     checkbox("God mode", player::isGod, player::setGod),
 *     hint("Changes apply at once.")));
 * }</pre>
 */
public final class FlixelDebugUi {

  private FlixelDebugUi() {}

  /**
   * Creates a column: the widgets top to bottom.
   *
   * @param children The widgets, in order.
   * @return The column.
   */
  @NotNull
  public static FlixelDebugStack column(@NotNull FlixelDebugWidget<?>... children) {
    return new FlixelDebugStack(false).add(children);
  }

  /**
   * Creates a row: the widgets left to right.
   *
   * @param children The widgets, in order.
   * @return The row.
   */
  @NotNull
  public static FlixelDebugStack row(@NotNull FlixelDebugWidget<?>... children) {
    return new FlixelDebugStack(true).add(children);
  }

  /**
   * Creates a wrap: the widgets left to right, flowing onto new lines when the width runs out.
   *
   * @param children The widgets, in order.
   * @return The wrap.
   */
  @NotNull
  public static FlixelDebugWrap wrap(@NotNull FlixelDebugWidget<?>... children) {
    return new FlixelDebugWrap().add(children);
  }

  /**
   * Creates a scroll area around a widget.
   *
   * @param child The widget that scrolls.
   * @return The scroll area.
   */
  @NotNull
  public static FlixelDebugScroll scroll(@NotNull FlixelDebugWidget<?> child) {
    return new FlixelDebugScroll(child);
  }

  /**
   * Creates a foldable section around a widget.
   *
   * @param header The text on the header.
   * @param child The widget the expander shows or hides.
   * @return The expander.
   */
  @NotNull
  public static FlixelDebugExpander expander(@NotNull CharSequence header, @NotNull FlixelDebugWidget<?> child) {
    return new FlixelDebugExpander(header, child);
  }

  /**
   * Creates a label.
   *
   * @param text The text.
   * @return The label.
   */
  @NotNull
  public static FlixelDebugLabel label(@NotNull CharSequence text) {
    return new FlixelDebugLabel(text);
  }

  /**
   * Creates a label that writes its own text every frame.
   *
   * @param appender Writes the text into the cleared holder it receives. It must not allocate.
   * @return The label.
   */
  @NotNull
  public static FlixelDebugLabel label(@NotNull Consumer<FlixelString> appender) {
    return new FlixelDebugLabel(appender);
  }

  /**
   * Creates a heading: a label in a larger, bold font.
   *
   * @param text The text.
   * @return The label.
   */
  @NotNull
  public static FlixelDebugLabel heading(@NotNull CharSequence text) {
    return new FlixelDebugLabel(text).setStyle(FlixelDebugProtocol.STYLE_HEADING);
  }

  /**
   * Creates a hint: muted text that wraps onto several lines.
   *
   * @param text The text.
   * @return The label.
   */
  @NotNull
  public static FlixelDebugLabel hint(@NotNull CharSequence text) {
    return new FlixelDebugLabel(text).setStyle(FlixelDebugProtocol.TONE_MUTED).setWrap(true);
  }

  /**
   * Creates a button.
   *
   * @param text The text on the button.
   * @param onClick What runs on a click.
   * @return The button.
   */
  @NotNull
  public static FlixelDebugButton button(@NotNull CharSequence text, @Nullable Runnable onClick) {
    return new FlixelDebugButton(text, onClick);
  }

  /**
   * Creates a checkbox bound to a setting.
   *
   * @param text The text next to the box.
   * @param getter Reads the setting every frame.
   * @param onChange Changes the setting when the person clicks.
   * @return The checkbox.
   */
  @NotNull
  public static FlixelDebugCheckbox checkbox(@NotNull CharSequence text, @Nullable BooleanSupplier getter,
      @Nullable BooleanConsumer onChange) {
    return new FlixelDebugCheckbox(text, getter, onChange);
  }

  /**
   * Creates a toggle button bound to a setting.
   *
   * @param text The text on the toggle.
   * @param getter Reads the setting every frame.
   * @param onChange Changes the setting when the person clicks.
   * @return The toggle.
   */
  @NotNull
  public static FlixelDebugToggle toggle(@NotNull CharSequence text, @Nullable BooleanSupplier getter,
      @Nullable BooleanConsumer onChange) {
    return new FlixelDebugToggle(text, getter, onChange);
  }

  /**
   * Creates a one-line text box.
   *
   * @param placeholder The text shown while the box is empty.
   * @return The text box.
   */
  @NotNull
  public static FlixelDebugTextBox textBox(@Nullable String placeholder) {
    return new FlixelDebugTextBox().setPlaceholder(placeholder);
  }

  /**
   * Creates a number field bound to a value.
   *
   * @param getter Reads the value every frame.
   * @param onChange Receives the value the person entered.
   * @return The number field.
   */
  @NotNull
  public static FlixelDebugNumber number(@Nullable DoubleSupplier getter, @Nullable DoubleConsumer onChange) {
    return new FlixelDebugNumber(getter, onChange);
  }

  /**
   * Creates a slider bound to a value.
   *
   * @param min The smallest value.
   * @param max The largest value.
   * @param getter Reads the value every frame.
   * @param onChange Receives the value the person chose.
   * @return The slider.
   */
  @NotNull
  public static FlixelDebugSlider slider(double min, double max, @Nullable DoubleSupplier getter,
      @Nullable DoubleConsumer onChange) {
    FlixelDebugSlider slider = new FlixelDebugSlider(getter, onChange);
    slider.setRange(min, max);
    return slider;
  }

  /**
   * Creates a separator line.
   *
   * @return The separator.
   */
  @NotNull
  public static FlixelDebugSeparator separator() {
    return new FlixelDebugSeparator();
  }
}
