/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.group;

import com.badlogic.gdx.utils.SnapshotArray;

import me.stringdotjar.flixelgdx.FlixelBasic;

import org.jetbrains.annotations.Nullable;

/**
 * A {@link FlixelGroupable} whose members are {@link FlixelBasic} instances. Engine code (overlap checks, debug
 * traversal, {@link FlixelBasicGroup}) can depend on this marker and the helpers below without forcing generic
 * {@link FlixelGroup} users to extend {@link FlixelBasic}.
 *
 * <p>For lifecycle guidance ({@code kill}/{@code revive}/{@code destroy}), see {@link FlixelBasic}.
 *
 * @param <T> Member type.
 */
public interface FlixelBasicGroupable<T extends FlixelBasic> extends FlixelGroupable<T> {

  /**
   * Removes the member from the group; if {@code destroy} is {@code true}, also calls {@link FlixelBasic#destroy()} on it
   * after removal.
   *
   * @param member The member to remove.
   * @param destroy If {@code true}, call {@link FlixelBasic#destroy()} after unlinking.
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
        if (m != null && !m.exists) {
          return m;
        }
      }
    } finally {
      members.end();
    }
    return null;
  }
}
