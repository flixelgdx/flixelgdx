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
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A field the person types into, from a one-line filter to a full command line.
 *
 * <p>Typing happens in the app, so it never waits for the game. The game hears about the text in two ways:
 *
 * <ul>
 *   <li>{@link #onSubmit(Consumer)} runs when the person presses Enter (one-line boxes only).</li>
 *   <li>{@link #onChange(Consumer)} runs while the person types, but only when {@link #setLive(boolean) live} is on.
 *       The app sends at most a few changes a second.</li>
 * </ul>
 *
 * <p>A text box can also act as a command line. Give it {@linkplain #beginCompletions() completions} (name, usage,
 * and description of every command) and the app suggests matching names under the box and completes them with Tab.
 * With {@link #setHistory(int)}, Up and Down bring back earlier lines. Both run inside the app, so they are instant,
 * but the commands themselves still come from the game.
 *
 * <pre>{@code
 * FlixelDebugTextBox input = new FlixelDebugTextBox()
 *     .setPlaceholder("Type a command and press Enter")
 *     .setClearOnSubmit(true)
 *     .setHistory(64)
 *     .onSubmit(line -> Flixel.debug.console.run(line));
 * }</pre>
 *
 * <p>A {@link FlixelDebugTable} or {@link FlixelDebugTree} can use a text box as its {@linkplain
 * FlixelDebugTable#setFilter(FlixelDebugTextBox) filter}; the app then hides the rows that do not match, without a
 * round trip to the game.
 */
public class FlixelDebugTextBox extends FlixelDebugWidget<FlixelDebugTextBox> {

  private final FlixelString text = new FlixelString(16);
  private final FlixelDebugListBuffer completions = new FlixelDebugListBuffer();
  private String placeholder = "";
  private Consumer<CharSequence> onChange;
  private Consumer<CharSequence> onSubmit;
  private int history;

  private boolean multiline;
  private boolean readOnly;
  private boolean clearOnSubmit;
  private boolean live;

  /** Creates an empty text box. */
  public FlixelDebugTextBox() {
    super(FlixelDebugProtocol.WIDGET_TEXT_BOX);
  }

  /**
   * Sets what runs when the person presses Enter.
   *
   * @param onSubmit Receives the submitted text (a reused buffer; call {@code toString()} to keep it), or
   *   {@code null} for nothing.
   * @return This text box, for chaining.
   */
  @NotNull
  public FlixelDebugTextBox onSubmit(@Nullable Consumer<CharSequence> onSubmit) {
    this.onSubmit = onSubmit;
    return this;
  }

  /**
   * Sets what runs while the person types. It only runs when {@link #setLive(boolean)} is on.
   *
   * @param onChange Receives the current text (a reused buffer), or {@code null} for nothing.
   * @return This text box, for chaining.
   */
  @NotNull
  public FlixelDebugTextBox onChange(@Nullable Consumer<CharSequence> onChange) {
    this.onChange = onChange;
    return this;
  }

  /**
   * Starts replacing the completions. For every command write a list with three strings, the name, the usage, and
   * the description, then call {@link #endCompletions(int)}:
   *
   * <pre>{@code
   * FlixelDebugMessageWriter w = box.beginCompletions();
   * w.beginList().writeValue("god").writeValue("god [on|off]").writeValue("Makes the player invincible.").endList(3);
   * box.endCompletions(1);
   * }</pre>
   *
   * @return The writer to write the completions into.
   */
  @NotNull
  public FlixelDebugMessageWriter beginCompletions() {
    return completions.begin();
  }

  /**
   * Finishes replacing the completions. They are only sent when they changed.
   *
   * @param count How many completions were written.
   * @return This text box, for chaining.
   */
  @NotNull
  public FlixelDebugTextBox endCompletions(int count) {
    if (completions.end(count)) {
      markDirty(FlixelDebugProtocol.PROP_COMPLETIONS);
    }
    return this;
  }

  @Override
  protected long getPropMask() {
    long mask = super.getPropMask();
    if (!text.isEmpty()) {
      mask |= bit(FlixelDebugProtocol.PROP_TEXT);
    }
    if (!placeholder.isEmpty()) {
      mask |= bit(FlixelDebugProtocol.PROP_PLACEHOLDER);
    }
    if (multiline) {
      mask |= bit(FlixelDebugProtocol.PROP_MULTILINE);
    }
    if (readOnly) {
      mask |= bit(FlixelDebugProtocol.PROP_READ_ONLY);
    }
    if (clearOnSubmit) {
      mask |= bit(FlixelDebugProtocol.PROP_CLEAR_ON_SUBMIT);
    }
    if (history != 0) {
      mask |= bit(FlixelDebugProtocol.PROP_HISTORY);
    }
    if (completions.getCount() > 0) {
      mask |= bit(FlixelDebugProtocol.PROP_COMPLETIONS);
    }
    if (live) {
      mask |= bit(FlixelDebugProtocol.PROP_LIVE);
    }
    return mask;
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    switch (key) {
      case FlixelDebugProtocol.PROP_TEXT -> writer.writeValue(text);
      case FlixelDebugProtocol.PROP_PLACEHOLDER -> writer.writeValue(placeholder);
      case FlixelDebugProtocol.PROP_MULTILINE -> writer.writeValue(multiline);
      case FlixelDebugProtocol.PROP_READ_ONLY -> writer.writeValue(readOnly);
      case FlixelDebugProtocol.PROP_CLEAR_ON_SUBMIT -> writer.writeValue(clearOnSubmit);
      case FlixelDebugProtocol.PROP_HISTORY -> writer.writeValue(history);
      case FlixelDebugProtocol.PROP_COMPLETIONS -> completions.write(writer);
      case FlixelDebugProtocol.PROP_LIVE -> writer.writeValue(live);
      default -> super.writeProp(writer, key);
    }
  }

  @Override
  protected void handleEvent(int event, @NotNull FlixelDebugEventArgs args) {
    if (event != FlixelDebugProtocol.EVENT_CHANGE && event != FlixelDebugProtocol.EVENT_SUBMIT) {
      return;
    }
    CharSequence value = args.nextText();
    if (value == null || readOnly) {
      return;
    }
    // The app already shows this text, so it is stored without sending it back.
    text.set(value);
    if (event == FlixelDebugProtocol.EVENT_CHANGE) {
      if (onChange != null) {
        onChange.accept(text);
      }
      return;
    }
    if (multiline) {
      return;
    }
    if (onSubmit != null) {
      onSubmit.accept(text);
    }
    if (clearOnSubmit) {
      text.clear();
    }
  }

  @NotNull
  public CharSequence getText() {
    return text;
  }

  /**
   * Replaces the text the person sees, which also replaces whatever they typed.
   *
   * @param text The new text, or {@code null} for none.
   * @return This text box, for chaining.
   */
  @NotNull
  public FlixelDebugTextBox setText(@Nullable CharSequence text) {
    if (copyText(this.text, text)) {
      markDirty(FlixelDebugProtocol.PROP_TEXT);
    }
    return this;
  }

  @NotNull
  public String getPlaceholder() {
    return placeholder;
  }

  /**
   * Sets the faint hint shown while the box is empty.
   *
   * @param placeholder The hint, or {@code null} for none.
   * @return This text box, for chaining.
   */
  @NotNull
  public FlixelDebugTextBox setPlaceholder(@Nullable String placeholder) {
    String value = placeholder != null ? placeholder : "";
    if (!this.placeholder.equals(value)) {
      this.placeholder = value;
      markDirty(FlixelDebugProtocol.PROP_PLACEHOLDER);
    }
    return this;
  }

  public boolean isMultiline() {
    return multiline;
  }

  /**
   * Sets whether the box accepts several lines. A multiline box never submits.
   *
   * @param multiline {@code true} for several lines.
   * @return This text box, for chaining.
   */
  @NotNull
  public FlixelDebugTextBox setMultiline(boolean multiline) {
    if (this.multiline != multiline) {
      this.multiline = multiline;
      markDirty(FlixelDebugProtocol.PROP_MULTILINE);
    }
    return this;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  /**
   * Sets whether the person can only read and copy the text.
   *
   * @param readOnly {@code true} to block typing.
   * @return This text box, for chaining.
   */
  @NotNull
  public FlixelDebugTextBox setReadOnly(boolean readOnly) {
    if (this.readOnly != readOnly) {
      this.readOnly = readOnly;
      markDirty(FlixelDebugProtocol.PROP_READ_ONLY);
    }
    return this;
  }

  public boolean isClearOnSubmit() {
    return clearOnSubmit;
  }

  /**
   * Sets whether the app empties the box after the person submits it, like a chat input.
   *
   * @param clearOnSubmit {@code true} to empty it.
   * @return This text box, for chaining.
   */
  @NotNull
  public FlixelDebugTextBox setClearOnSubmit(boolean clearOnSubmit) {
    if (this.clearOnSubmit != clearOnSubmit) {
      this.clearOnSubmit = clearOnSubmit;
      markDirty(FlixelDebugProtocol.PROP_CLEAR_ON_SUBMIT);
    }
    return this;
  }

  public int getHistory() {
    return history;
  }

  /**
   * Sets how many submitted lines Up and Down bring back. The app keeps them.
   *
   * @param history The number of lines, or {@code 0} to turn it off.
   * @return This text box, for chaining.
   */
  @NotNull
  public FlixelDebugTextBox setHistory(int history) {
    if (this.history != history) {
      this.history = history;
      markDirty(FlixelDebugProtocol.PROP_HISTORY);
    }
    return this;
  }

  public boolean isLive() {
    return live;
  }

  /**
   * Sets whether the app reports the text while the person types, not only on Enter. A text box that filters a
   * table does not need it, because the app filters on its own.
   *
   * @param live {@code true} to receive changes while typing.
   * @return This text box, for chaining.
   */
  @NotNull
  public FlixelDebugTextBox setLive(boolean live) {
    if (this.live != live) {
      this.live = live;
      markDirty(FlixelDebugProtocol.PROP_LIVE);
    }
    return this;
  }
}
