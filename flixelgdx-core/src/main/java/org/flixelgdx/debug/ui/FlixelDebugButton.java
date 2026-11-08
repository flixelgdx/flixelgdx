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

/**
 * A button that runs game code when the person clicks it.
 *
 * <p>The click travels from the app to the game, and {@link #onClick(Runnable)} runs on the game's main thread, so it
 * may touch any game object. For a destructive button, set a {@linkplain #setConfirm(String) question}: the app asks
 * it first and only sends the click when the person agrees.
 *
 * <pre>{@code
 * FlixelDebugButton erase = new FlixelDebugButton("Erase save", save::erase)
 *     .setConfirm("Erase all save data? This cannot be undone.")
 *     .setStyle(FlixelDebugProtocol.TONE_ERROR);
 * }</pre>
 */
public class FlixelDebugButton extends FlixelDebugWidget<FlixelDebugButton> {

  private final FlixelString text = new FlixelString(16);
  private String confirm = "";
  private Runnable onClick;

  /**
   * Creates a button that does nothing until {@link #onClick(Runnable)} is set.
   *
   * @param text The text on the button.
   */
  public FlixelDebugButton(@NotNull CharSequence text) {
    this(text, null);
  }

  /**
   * Creates a button.
   *
   * @param text The text on the button.
   * @param onClick What runs on a click, or {@code null} for nothing.
   */
  public FlixelDebugButton(@NotNull CharSequence text, @Nullable Runnable onClick) {
    super(FlixelDebugProtocol.WIDGET_BUTTON);
    this.text.set(text);
    this.onClick = onClick;
  }

  /** Runs the click action as if the person clicked the button. Does nothing while the button is disabled. */
  public void click() {
    if (isEnabled() && onClick != null) {
      onClick.run();
    }
  }

  /**
   * Sets what runs on a click.
   *
   * @param onClick The action, or {@code null} for nothing.
   * @return This button, for chaining.
   */
  @NotNull
  public FlixelDebugButton onClick(@Nullable Runnable onClick) {
    this.onClick = onClick;
    return this;
  }

  @Override
  protected long getPropMask() {
    long mask = super.getPropMask() | bit(FlixelDebugProtocol.PROP_TEXT);
    if (!confirm.isEmpty()) {
      mask |= bit(FlixelDebugProtocol.PROP_CONFIRM);
    }
    return mask;
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    switch (key) {
      case FlixelDebugProtocol.PROP_TEXT -> writer.writeValue(text);
      case FlixelDebugProtocol.PROP_CONFIRM -> writer.writeValue(confirm);
      default -> super.writeProp(writer, key);
    }
  }

  @Override
  protected void handleEvent(int event, @NotNull FlixelDebugEventArgs args) {
    if (event == FlixelDebugProtocol.EVENT_CLICK) {
      click();
    }
  }

  @NotNull
  public CharSequence getText() {
    return text;
  }

  /**
   * Sets the text on the button. The text is copied, so a reused buffer is safe to pass.
   *
   * @param text The new text, or {@code null} for none.
   * @return This button, for chaining.
   */
  @NotNull
  public FlixelDebugButton setText(@Nullable CharSequence text) {
    if (copyText(this.text, text)) {
      markDirty(FlixelDebugProtocol.PROP_TEXT);
    }
    return this;
  }

  @NotNull
  public String getConfirm() {
    return confirm;
  }

  /**
   * Sets a question the app asks before it sends a click, such as {@code "Erase all save data?"}.
   *
   * @param confirm The question, or {@code null} or empty to click without asking.
   * @return This button, for chaining.
   */
  @NotNull
  public FlixelDebugButton setConfirm(@Nullable String confirm) {
    String value = confirm != null ? confirm : "";
    if (!this.confirm.equals(value)) {
      this.confirm = value;
      markDirty(FlixelDebugProtocol.PROP_CONFIRM);
    }
    return this;
  }
}
