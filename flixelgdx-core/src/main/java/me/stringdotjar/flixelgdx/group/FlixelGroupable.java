/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.group;

import com.badlogic.gdx.utils.SnapshotArray;

/**
 * Interface for group-like containers that hold a list of members. Both
 * {@link FlixelGroup} and {@link FlixelSpriteGroup} implement this, allowing
 * generic traversal of the object tree (e.g. for debug utilities).
 *
 * <p>This interface is intentionally engine-agnostic. It does not require members to extend
 * any FlixelGDX base class. Engine systems that need {@link me.stringdotjar.flixelgdx.FlixelBasic} behavior and
 * mandatory pooling should depend on {@link FlixelBasicGroupable} instead.
 *
 * @param <T> The member type.
 */
public interface FlixelGroupable<T> {

  /**
   * Adds a member to this group. For {@link FlixelBasicGroupable} implementations, members removed via
   * {@link #remove} are returned to {@link FlixelBasicGroupable#getMemberPool()}; prefer obtaining them with
   * {@link FlixelBasicGroupable#obtainMember()} or {@link FlixelGroup#recycle()} so {@link #remove} stays consistent.
   */
  void add(T member);

  /**
   * Removes a member and, for {@link FlixelBasicGroupable} groups, returns it to the mandatory pool after destroy.
   */
  void remove(T member);

  /** Removes all members. For pooled groups, each member is returned to the pool. */
  void clear();

  /** Returns the backing array of members. */
  SnapshotArray<T> getMembers();

  /**
   * Returns the maximum number of members allowed. When {@code 0}, the group
   * can grow without limit.
   */
  int getMaxSize();

  /**
   * Sets the maximum number of members allowed. Values less than {@code 0}
   * are clamped to {@code 0} (unlimited).
   */
  void setMaxSize(int maxSize);
}
