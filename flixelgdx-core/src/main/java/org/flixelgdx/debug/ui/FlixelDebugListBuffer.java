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
import org.jetbrains.annotations.NotNull;

/**
 * The encoded items of one {@code LIST} property (such as the rows of a table) that only reports a change when the
 * bytes really differ.
 *
 * <p>Data widgets rebuild their lists every time the game refreshes them, but most of the time nothing changed. This
 * buffer keeps two copies, the one the app has and the one being built, and compares them byte by byte when building
 * ends. Think of it as tracing a drawing on a light table: you redraw the picture on a fresh sheet, hold it over the
 * old one, and only mail it if the lines moved. Nothing is allocated once both sheets are big enough.
 *
 * <pre>{@code
 * FlixelDebugMessageWriter items = list.begin();
 * items.writeValue("Easy").writeValue("Hard");
 * if (list.end(2)) {
 *   markDirty(FlixelDebugProtocol.PROP_ITEMS);
 * }
 * // Later, when the property is written:
 * list.write(writer);
 * }</pre>
 */
public final class FlixelDebugListBuffer {

  private FlixelDebugMessageWriter current = new FlixelDebugMessageWriter(64);
  private FlixelDebugMessageWriter next = new FlixelDebugMessageWriter(64);
  private int count;

  /** Creates an empty list. */
  public FlixelDebugListBuffer() {}

  /**
   * Starts building a new version of the list.
   *
   * @return The writer to write the items into, as values. Do not start a message on it.
   */
  @NotNull
  public FlixelDebugMessageWriter begin() {
    return next.clear();
  }

  /**
   * Finishes building and keeps the new version when it differs from the old one.
   *
   * @param itemCount How many items were written since {@link #begin()}.
   * @return {@code true} when the list changed.
   */
  public boolean end(int itemCount) {
    if (itemCount == count && next.contentEquals(current)) {
      return false;
    }
    FlixelDebugMessageWriter swap = current;
    current = next;
    next = swap;
    count = itemCount;
    return true;
  }

  /** Empties the list. */
  public void clear() {
    begin();
    end(0);
  }

  /**
   * Writes the list as one {@code LIST} value.
   *
   * @param writer The writer to write into.
   */
  public void write(@NotNull FlixelDebugMessageWriter writer) {
    writer.beginList().writeRaw(current).endList(count);
  }

  /**
   * Returns how many items the current version holds.
   *
   * @return The count.
   */
  public int getCount() {
    return count;
  }
}
