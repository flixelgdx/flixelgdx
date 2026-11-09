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
package org.flixelgdx.debug;

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.group.FlixelGroupable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reads the children of any object the debugger can walk, whether it is a {@link FlixelDebugNode} or a
 * {@link FlixelGroupable}.
 *
 * <p>Think of it as one master key for every door: the scene panel, the path resolver, the picker, and the
 * inspector all ask "how many children, and which one is number three?" without caring how the object stores
 * them. A {@link FlixelDebugNode} is asked first, then a group, and anything else has no children.
 *
 * <p>Nothing here allocates.
 *
 * @see FlixelDebugNode
 */
public final class FlixelDebugNodes {

  private FlixelDebugNodes() {}

  /**
   * Returns how many children an object has, counting empty slots.
   *
   * @param node The object.
   * @return The child count, zero when the object is neither a debug node nor a group.
   */
  public static int childCount(@Nullable Object node) {
    if (node instanceof FlixelDebugNode debug) {
      return Math.max(0, debug.getDebugChildCount());
    }
    if (node instanceof FlixelGroupable<?> group) {
      FlixelArray<?> members = group.getMembers();
      return members != null ? members.getSize() : 0;
    }
    return 0;
  }

  /**
   * Returns one child of an object.
   *
   * @param node The object.
   * @param index A position from 0 to {@link #childCount(Object)} minus 1.
   * @return The child, or {@code null} for an empty slot or an index out of range.
   */
  @Nullable
  public static Object child(@NotNull Object node, int index) {
    if (index < 0) {
      return null;
    }
    if (node instanceof FlixelDebugNode debug) {
      return index < debug.getDebugChildCount() ? debug.getDebugChild(index) : null;
    }
    if (node instanceof FlixelGroupable<?> group) {
      FlixelArray<?> members = group.getMembers();
      return members != null && index < members.getSize() ? members.get(index) : null;
    }
    return null;
  }

  /**
   * Returns whether an object can have children the debugger walks.
   *
   * @param node The object.
   * @return {@code true} for a {@link FlixelDebugNode} or a {@link FlixelGroupable}.
   */
  public static boolean isContainer(@Nullable Object node) {
    return node instanceof FlixelDebugNode || node instanceof FlixelGroupable<?>;
  }
}
