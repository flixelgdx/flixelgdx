/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.group;

import com.badlogic.gdx.utils.ArraySupplier;
import com.badlogic.gdx.utils.SnapshotArray;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Framework-agnostic member list backed by a {@link SnapshotArray}. Use this in a plain libGDX {@code Screen} or game
 * loop with <em>any</em> member type ({@code FlixelGroup<Actor>}, {@code FlixelGroup<YourEntity>}, etc.): call
 * {@link #add}, {@link #remove}, and {@link #forEachMember} yourself; there is no automatic {@code update}/{@code draw}.
 *
 * <p>For FlixelGDX objects that extend {@link me.stringdotjar.flixelgdx.FlixelBasic}, use {@link FlixelBasicGroup} or
 * {@link me.stringdotjar.flixelgdx.FlixelState} instead so members receive the usual update/draw/recycle lifecycle.
 *
 * @param <T> Member type (unconstrained).
 * @see FlixelBasicGroup
 */
public class FlixelGroup<T> implements FlixelGroupable<T> {

  protected SnapshotArray<T> members;

  private final ArraySupplier<T[]> memberArrayFactory;

  protected int maxSize = 0;

  /**
   * Creates a new group with the given array factory and unlimited size by default.
   *
   * @param arrayFactory The array factory to use.
   */
  public FlixelGroup(@NotNull ArraySupplier<T[]> arrayFactory) {
    this(arrayFactory, 0);
  }

  /**
   * Creates a new group with the given array factory and maximum size.
   *
   * @param arrayFactory The array factory to use.
   * @param maxSize The maximum size of the group.
   */
  public FlixelGroup(@NotNull ArraySupplier<T[]> arrayFactory, int maxSize) {
    this.memberArrayFactory = Objects.requireNonNull(arrayFactory, "Array factory cannot be null");
    this.maxSize = Math.max(0, maxSize);
    members = new SnapshotArray<>(arrayFactory);
  }

  public void ensureMembers() {
    if (members == null) {
      members = new SnapshotArray<>(memberArrayFactory);
    }
  }

  @Override
  public void add(T member) {
    if (member == null) {
      return;
    }
    ensureMembers();
    if (maxSize > 0 && members.size >= maxSize) {
      return;
    }
    members.add(member);
  }

  @Override
  public void remove(T member) {
    if (member == null || members == null) {
      return;
    }
    members.removeValue(member, true);
  }

  @Override
  public void clear() {
    if (members == null) {
      return;
    }
    members.clear();
  }

  /**
   * Clears the member list and discards the backing {@link SnapshotArray} so the next {@link #ensureMembers()} or
   * {@link #add} allocates a fresh array. Does not call any method on member instances.
   */
  public void resetStorage() {
    if (members != null) {
      members.clear();
      members = null;
    }
  }

  @Override
  @Nullable
  public SnapshotArray<T> getMembers() {
    return members;
  }

  @Override
  public int getMaxSize() {
    return maxSize;
  }

  @Override
  public void setMaxSize(int maxSize) {
    this.maxSize = Math.max(0, maxSize);
  }
}
