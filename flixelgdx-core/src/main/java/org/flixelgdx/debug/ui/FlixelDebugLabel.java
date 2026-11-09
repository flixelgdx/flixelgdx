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
 * A piece of text, such as a title, a value, or a hint.
 *
 * <p>Labels are the plainest widget: they show text and nothing else. Give one a {@linkplain #setStyle(int) style}
 * to make it a heading, a muted hint, or a red error. The person can select and copy the text.
 *
 * <p>A label can also fill its own text every frame from an <em>appender</em>, a function that writes into a reused
 * {@link FlixelString}. That way a changing value never allocates a new {@code String}:
 *
 * <pre>{@code
 * FlixelDebugLabel fps = new FlixelDebugLabel(out -> out.concat("FPS: ").concat(Flixel.graphics.getFps()));
 * }</pre>
 */
public class FlixelDebugLabel extends FlixelDebugWidget<FlixelDebugLabel> implements FlixelDebugBound {

  private final FlixelString text = new FlixelString(16);
  private final FlixelString scratch;
  private final Consumer<FlixelString> appender;

  private boolean wrap;

  /** Creates an empty label. */
  public FlixelDebugLabel() {
    this("");
  }

  /**
   * Creates a label with fixed text.
   *
   * @param text The text.
   */
  public FlixelDebugLabel(@NotNull CharSequence text) {
    super(FlixelDebugProtocol.WIDGET_LABEL);
    this.text.set(text);
    this.scratch = null;
    this.appender = null;
  }

  /**
   * Creates a label that writes its own text every frame.
   *
   * @param appender Writes the text into the cleared holder it receives. It must not allocate.
   */
  public FlixelDebugLabel(@NotNull Consumer<FlixelString> appender) {
    super(FlixelDebugProtocol.WIDGET_LABEL);
    this.scratch = new FlixelString(16);
    this.appender = appender;
  }

  @Override
  public void refresh() {
    if (appender != null) {
      scratch.clear();
      appender.accept(scratch);
      setText(scratch);
    }
  }

  @Override
  protected long getPropMask() {
    long mask = super.getPropMask();
    if (!text.isEmpty()) {
      mask |= bit(FlixelDebugProtocol.PROP_TEXT);
    }
    if (wrap) {
      mask |= bit(FlixelDebugProtocol.PROP_WRAP);
    }
    return mask;
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    switch (key) {
      case FlixelDebugProtocol.PROP_TEXT -> writer.writeValue(text);
      case FlixelDebugProtocol.PROP_WRAP -> writer.writeValue(wrap);
      default -> super.writeProp(writer, key);
    }
  }

  @NotNull
  public CharSequence getText() {
    return text;
  }

  /**
   * Replaces the text. The text is copied, so a reused {@link FlixelString} can be passed every frame.
   *
   * @param text The new text, or {@code null} for none.
   * @return This label, for chaining.
   */
  @NotNull
  public FlixelDebugLabel setText(@Nullable CharSequence text) {
    if (copyText(this.text, text)) {
      markDirty(FlixelDebugProtocol.PROP_TEXT);
    }
    return this;
  }

  public boolean isWrap() {
    return wrap;
  }

  /**
   * Sets whether long text breaks into several lines. Without wrapping, text that does not fit ends with an
   * ellipsis.
   *
   * @param wrap {@code true} to wrap.
   * @return This label, for chaining.
   */
  @NotNull
  public FlixelDebugLabel setWrap(boolean wrap) {
    if (this.wrap != wrap) {
      this.wrap = wrap;
      markDirty(FlixelDebugProtocol.PROP_WRAP);
    }
    return this;
  }
}
