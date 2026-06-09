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
package org.flixelgdx.util;

import com.badlogic.gdx.utils.SnapshotArray;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelState;
import org.flixelgdx.debug.FlixelDebugDrawable;
import org.flixelgdx.functional.FlixelVisible;
import org.flixelgdx.functional.IFlixelBasic;
import org.flixelgdx.group.FlixelGroupable;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Utility methods used by the debug overlay for recursively traversing the state's object
 * tree (counting active members, iterating {@link FlixelDebugDrawable} instances for bounding-box drawing, etc.).
 *
 * <p>Recursion descends into any member that implements {@link FlixelGroupable}, which
 * covers {@link org.flixelgdx.group.FlixelBasicGroup}, {@link org.flixelgdx.group.FlixelSpriteGroup},
 * and nested {@link org.flixelgdx.group.FlixelGroup} instances whose elements are {@link IFlixelBasic}.
 */
public final class FlixelDebugUtil {

  private FlixelDebugUtil() {}

  /**
   * Recursively counts all active members in the current state's object tree. A member is
   * counted when {@link IFlixelBasic#isExists()} is {@code true}.
   *
   * @return The number of active members, or {@code 0} if no state is loaded.
   */
  public static int countActiveMembers() {
    FlixelState state = Flixel.getState();
    if (state == null) {
      return 0;
    }
    return countActiveMembersRecursive(state.getMembers());
  }

  private static int countActiveMembersRecursive(SnapshotArray<?> members) {
    int count = 0;
    Object[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      Object o = items[i];
      if (!(o instanceof IFlixelBasic member)) {
        continue;
      }
      if (member.isExists()) {
        count++;
      }
      if (member instanceof FlixelGroupable<?> group) {
        SnapshotArray<?> nested = group.getMembers();
        if (nested != null) {
          count += countActiveMembersRecursive(nested);
        }
      }
    }
    members.end();
    return count;
  }

  /**
   * Iterates all visible {@link FlixelDebugDrawable} instances in the current state's
   * object tree (where {@link IFlixelBasic#isExists()} and {@link FlixelVisible#isVisible()} are both {@code true}),
   * invoking the callback for each one. No intermediate collection is created.
   *
   * @param callback Invoked once per visible {@link FlixelDebugDrawable}.
   */
  public static void forEachDebugDrawable(Consumer<FlixelDebugDrawable> callback) {
    FlixelState state = Flixel.getState();
    if (state == null) {
      return;
    }
    forEachDebugDrawableRecursive(state.getMembers(), callback);
  }

  private static void forEachDebugDrawableRecursive(@NotNull SnapshotArray<?> members,
      @NotNull Consumer<FlixelDebugDrawable> callback) {
    Object[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      Object o = items[i];
      if (!(o instanceof IFlixelBasic member)) {
        continue;
      }
      if (member instanceof FlixelDebugDrawable drawable && member.isExists() && member.isVisible()) {
        callback.accept(drawable);
      }
      if (member instanceof FlixelGroupable<?> group) {
        SnapshotArray<?> nested = group.getMembers();
        if (nested != null) {
          forEachDebugDrawableRecursive(nested, callback);
        }
      }
    }
    members.end();
  }

}
