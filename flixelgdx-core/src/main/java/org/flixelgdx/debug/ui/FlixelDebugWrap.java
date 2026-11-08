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
import org.jetbrains.annotations.NotNull;

/**
 * Lines its children up from left to right and starts a new line when the width runs out, like words in a paragraph.
 *
 * <p>A wrap is the right choice for a toolbar of small buttons and toggles: on a wide panel they sit in one line, on
 * a narrow one they flow onto a second line instead of being cut off.
 *
 * <pre>{@code
 * FlixelDebugWrap toolbar = FlixelDebugUi.wrap(debugToggle, infoToggle, warnToggle, clearButton);
 * }</pre>
 */
public class FlixelDebugWrap extends FlixelDebugContainer<FlixelDebugWrap> {

  private float spacing = FlixelDebugStack.DEFAULT_SPACING;

  /** Creates an empty wrap. */
  public FlixelDebugWrap() {
    super(FlixelDebugProtocol.WIDGET_WRAP, Integer.MAX_VALUE);
  }

  @Override
  protected long getPropMask() {
    long mask = super.getPropMask();
    if (spacing != FlixelDebugStack.DEFAULT_SPACING) {
      mask |= bit(FlixelDebugProtocol.PROP_SPACING);
    }
    return mask;
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    if (key == FlixelDebugProtocol.PROP_SPACING) {
      writer.writeValue(spacing);
    } else {
      super.writeProp(writer, key);
    }
  }

  public float getSpacing() {
    return spacing;
  }

  /**
   * Sets the gap between children.
   *
   * @param spacing The gap in pixels.
   * @return This wrap panel, for chaining.
   */
  @NotNull
  public FlixelDebugWrap setSpacing(float spacing) {
    if (this.spacing != spacing) {
      this.spacing = spacing;
      markDirty(FlixelDebugProtocol.PROP_SPACING);
    }
    return this;
  }
}
