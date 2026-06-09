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
package org.flixelgdx.group;

import com.badlogic.gdx.utils.SnapshotArray;

import org.flixelgdx.functional.IFlixelBasic;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link FlixelGroupable} whose members are {@link IFlixelBasic} instances. Engine code (overlap checks, debug
 * traversal, {@link FlixelBasicGroup}) can depend on this marker and the helpers below without forcing generic
 * {@link FlixelGroup} users to extend {@link org.flixelgdx.FlixelBasic}.
 *
 * <p>For lifecycle guidance ({@code kill}/{@code revive}/{@code destroy}), see {@link org.flixelgdx.FlixelBasic}.
 *
 * @param <T> Member type.
 */
public interface FlixelBasicGroupable<T extends IFlixelBasic> extends FlixelGroupable<T> {

  /**
   * Removes the member from the group; if {@code destroy} is {@code true}, also calls {@link org.flixelgdx.functional.FlixelDestroyable#destroy()} on it
   * after removal.
   *
   * @param member The member to remove.
   * @param destroy If {@code true}, call {@link org.flixelgdx.functional.FlixelDestroyable#destroy()} after unlinking.
   */
  default void removeMember(T member, boolean destroy) {
    if (member == null) {
      return;
    }
    SnapshotArray<T> members = getMembers();
    if (members == null || !members.contains(member, true)) {
      return;
    }
    remove(member);
    if (destroy) {
      member.destroy();
    }
  }

  /**
   * Returns the first non-null member with {@code exists == false}, or {@code null}.
   */
  @Nullable
  default T getFirstDead() {
    SnapshotArray<T> members = getMembers();
    if (members == null) {
      return null;
    }
    T[] items = members.begin();
    try {
      for (int i = 0, n = members.size; i < n; i++) {
        T m = items[i];
        if (m != null && !m.isExists()) {
          return m;
        }
      }
    } finally {
      members.end();
    }
    return null;
  }
}
