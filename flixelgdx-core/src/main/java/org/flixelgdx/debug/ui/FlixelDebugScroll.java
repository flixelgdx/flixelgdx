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
import org.jetbrains.annotations.NotNull;

/**
 * Lets its one child scroll when it is bigger than the space the panel gives it.
 *
 * <p>Wrap a long column in a scroll so a short panel still shows all of it. Tables, trees, property grids, and text
 * boxes scroll on their own and do not need one.
 *
 * <pre>{@code
 * panel.setContent(new FlixelDebugScroll(FlixelDebugUi.column(lots, of, widgets)));
 * }</pre>
 */
public class FlixelDebugScroll extends FlixelDebugContainer<FlixelDebugScroll> {

  /** Creates an empty scroll area. */
  public FlixelDebugScroll() {
    super(FlixelDebugProtocol.WIDGET_SCROLL, 1);
  }

  /**
   * Creates a scroll area around a widget.
   *
   * @param child The widget that scrolls.
   */
  public FlixelDebugScroll(@NotNull FlixelDebugWidget<?> child) {
    this();
    add(child);
  }
}
