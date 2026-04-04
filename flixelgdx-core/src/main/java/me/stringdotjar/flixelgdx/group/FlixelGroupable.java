/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.group;

import com.badlogic.gdx.utils.SnapshotArray;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

/**
 * Group-like containers with a typed member list. This interface is intentionally generic so libGDX projects can use
 * {@link FlixelGroup} with any member type without adopting {@link me.stringdotjar.flixelgdx.FlixelBasic}.
 *
 * <p>FlixelGDX gameplay code that uses lifecycle flags and {@link me.stringdotjar.flixelgdx.FlixelBasic#destroy()} should
 * implement {@link FlixelBasicGroupable} instead (or use {@link FlixelBasicGroup} / {@link FlixelSpriteGroup}).
 *
 * @param <T> Member type.
 */
public interface FlixelGroupable<T> {

  /** Adds a member to this group. */
  void add(T member);

  /**
   * Removes the member from this group only. Does not interpret or tear down the member; see {@link FlixelBasicGroupable}
   * for optional {@code destroy} semantics on {@link me.stringdotjar.flixelgdx.FlixelBasic} members.
   */
  void remove(T member);

  /** Removes all members from the group without touching member instances. */
  void clear();

  /**
   * Returns the backing array, or {@code null} if the implementation has not allocated it yet ({@link FlixelGroup}).
   */
  @Nullable
  SnapshotArray<T> getMembers();

  /**
   * Returns the maximum number of members allowed. When {@code 0}, the group can grow without limit.
   */
  int getMaxSize();

  /**
   * Sets the maximum number of members allowed. Values less than {@code 0} are clamped to {@code 0} (unlimited).
   */
  void setMaxSize(int maxSize);

  /**
   * Removes the member from the group without additional teardown.
   *
   * @param member The member to remove.
   */
  default void detach(T member) {
    SnapshotArray<T> members = getMembers();
    if (member == null || members == null) {
      return;
    }
    members.removeValue(member, true);
  }

  /** Index of the first {@code null} slot in {@link #getMembers()}, or {@code -1} if none. */
  default int getFirstNullIndex() {
    SnapshotArray<T> members = getMembers();
    if (members == null) {
      return -1;
    }
    T[] items = members.begin();
    try {
      for (int i = 0, n = members.size; i < n; i++) {
        if (items[i] == null) {
          return i;
        }
      }
    } finally {
      members.end();
    }
    return -1;
  }

  /**
   * Invokes {@code callback} for each non-null member using snapshot iteration.
   *
   * @param callback The callback to call for each member.
   */
  default void forEachMember(Consumer<T> callback) {
    if (callback == null) {
      return;
    }
    var members = getMembers();
    if (members == null) {
      return;
    }
    try {
      T[] items = members.begin();
      for (int i = 0, n = members.size; i < n; i++) {
        T member = items[i];
        if (member == null) {
          continue;
        }
        callback.accept(member);
      }
    } finally {
      members.end();
    }
  }

  /**
   * Invokes {@code callback} for each member assignable to {@code type}.
   *
   * @param <C> Member subtype.
   * @param type The type to check.
   * @param callback The callback.
   */
  default <C> void forEachMemberType(Class<C> type, Consumer<C> callback) {
    if (type == null || callback == null) {
      return;
    }
    var members = getMembers();
    if (members == null) {
      return;
    }
    try {
      T[] items = members.begin();
      for (int i = 0, n = members.size; i < n; i++) {
        T member = items[i];
        if (type.isInstance(member)) {
          callback.accept(type.cast(member));
        }
      }
    } finally {
      members.end();
    }
  }
}
