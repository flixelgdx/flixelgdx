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

import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.functional.consumer.BooleanConsumer;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * The shared base of widgets that hold one true or false value: {@link FlixelDebugCheckbox} and
 * {@link FlixelDebugToggle}.
 *
 * <p>Such a widget is a light switch for one setting of the game. Built with a getter and a setter, it stays in sync
 * on its own: the getter is read every frame (so the widget follows changes made by the game, such as a pause hotkey)
 * and the setter runs when the person clicks. When the game refuses a change (the getter still returns the old value
 * after the setter ran), the widget shows the old value again.
 *
 * @param <W> The widget's own class, so that setters can return it for chaining.
 */
public abstract class FlixelDebugBooleanWidget<W extends FlixelDebugBooleanWidget<W>> extends FlixelDebugWidget<W>
    implements FlixelDebugBound {

  private final FlixelString text = new FlixelString(16);
  private BooleanSupplier getter;
  private BooleanConsumer onChange;

  private boolean value;

  /**
   * Creates the widget.
   *
   * @param type The widget type, such as {@link FlixelDebugProtocol#WIDGET_CHECKBOX}.
   * @param text The text.
   * @param getter Reads the value every frame, or {@code null}.
   * @param onChange Runs when the person changes the value, or {@code null}.
   */
  protected FlixelDebugBooleanWidget(int type, @NotNull CharSequence text, @Nullable BooleanSupplier getter,
      @Nullable BooleanConsumer onChange) {
    super(type);
    this.text.set(text);
    this.getter = getter;
    this.onChange = onChange;
    if (getter != null) {
      value = getter.getAsBoolean();
    }
  }

  @Override
  public void refresh() {
    if (getter != null) {
      setValue(getter.getAsBoolean());
    }
  }

  /**
   * Sets what runs when the person changes the value.
   *
   * @param onChange Receives the new value, or {@code null} for nothing.
   * @return This widget, for chaining.
   */
  @NotNull
  public W onChange(@Nullable BooleanConsumer onChange) {
    this.onChange = onChange;
    return self();
  }

  /**
   * Sets the getter that is read every frame while the panel is open.
   *
   * @param getter The getter, or {@code null} to set the value by hand.
   * @return This widget, for chaining.
   */
  @NotNull
  public W bind(@Nullable BooleanSupplier getter) {
    this.getter = getter;
    return self();
  }

  @Override
  protected long getPropMask() {
    long mask = super.getPropMask() | bit(FlixelDebugProtocol.PROP_TEXT);
    if (value) {
      mask |= bit(FlixelDebugProtocol.PROP_VALUE);
    }
    return mask;
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    switch (key) {
      case FlixelDebugProtocol.PROP_TEXT -> writer.writeValue(text);
      case FlixelDebugProtocol.PROP_VALUE -> writer.writeValue(value);
      default -> super.writeProp(writer, key);
    }
  }

  @Override
  protected void handleEvent(int event, @NotNull FlixelDebugEventArgs args) {
    if (event != FlixelDebugProtocol.EVENT_CHANGE) {
      return;
    }
    boolean wanted = args.nextBoolean(value);
    // The app already shows the new value, so it is stored without sending it back. When the game refuses the
    // change, the getter (or the listener) sets the old value again, which then is a real change and is sent.
    value = wanted;
    if (onChange != null) {
      onChange.accept(wanted);
    }
    if (getter != null && getter.getAsBoolean() != wanted) {
      value = !wanted;
      resend(FlixelDebugProtocol.PROP_VALUE);
    }
  }

  @NotNull
  public CharSequence getText() {
    return text;
  }

  /**
   * Sets the text next to the box. The text is copied, so a reused buffer is safe to pass.
   *
   * @param text The new text, or {@code null} for none.
   * @return This widget, for chaining.
   */
  @NotNull
  public W setText(@Nullable CharSequence text) {
    if (copyText(this.text, text)) {
      markDirty(FlixelDebugProtocol.PROP_TEXT);
    }
    return self();
  }

  public boolean getValue() {
    return value;
  }

  /**
   * Sets whether the widget is checked.
   *
   * @param value {@code true} to check it.
   * @return This widget, for chaining.
   */
  @NotNull
  public W setValue(boolean value) {
    if (this.value != value) {
      this.value = value;
      markDirty(FlixelDebugProtocol.PROP_VALUE);
    }
    return self();
  }
}
