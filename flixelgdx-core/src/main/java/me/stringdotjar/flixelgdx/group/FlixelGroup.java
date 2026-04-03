/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.group;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.ArraySupplier;
import com.badlogic.gdx.utils.SnapshotArray;

import me.stringdotjar.flixelgdx.FlixelBasic;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for creating groups with a list of members inside it.
 *
 * <p><b>Owned vs pooled members:</b> {@link #remove} calls {@link FlixelBasic#destroy()} (group-owned
 * teardown). For pooled instances, use {@link #detach} or {@link #removeMember} with {@code destroy == false},
 * then {@link com.badlogic.gdx.utils.Pool#free free} the member yourself. {@link #recycle} revives the first
 * non-existent member or {@link #add}s a new one from the factory.
 */
public abstract class FlixelGroup<T extends FlixelBasic> extends FlixelBasic implements FlixelBasicGroupable<T> {

  /**
   * The list of members that {@code this} group contains.
   */
  protected SnapshotArray<T> members;

  /** Used to recreate {@link #members} after {@link #destroy()} when this group is reused (e.g. root state). */
  private final ArraySupplier<T[]> memberArrayFactory;

  /**
   * Maximum number of members allowed. When {@code 0}, the group can grow without limit (default).
   * When {@code > 0}, {@link #add} will not add if at capacity.
   */
  protected int maxSize = 0;

  /**
   * Creates a new FlixelGroup with no maximum size.
   */
  protected FlixelGroup(ArraySupplier<T[]> arrayFactory) {
    this(arrayFactory, 0);
  }

  /**
   * Creates a new FlixelGroup with the given maximum size.
   *
   * @param memberType The runtime class of {@code T} used for array allocation.
   * @param maxSize Maximum number of members allowed. When {@code 0}, the group can grow without limit (default).
   * When {@code > 0}, {@link #add} will not add if at capacity.
   */
  protected FlixelGroup(ArraySupplier<T[]> arrayFactory, int maxSize) {
    this.memberArrayFactory = arrayFactory;
    this.maxSize = Math.max(0, maxSize);
    members = new SnapshotArray<>(arrayFactory);
  }

  /**
   * Recreates {@link #members} if {@link #destroy()} has run (members is {@code null}).
   */
  public void ensureMembers() {
    if (members == null) {
      members = new SnapshotArray<>(memberArrayFactory);
    }
  }

  @Override
  public void update(float elapsed) {
    if (members == null) {
      return;
    }

    T[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      T member = items[i];
      if (member == null) {
        continue;
      }
      if (!member.exists || !member.active) {
        continue;
      }
      member.update(elapsed);
    }
    members.end();
  }

  @Override
  public void draw(Batch batch) {
    if (members == null) {
      return;
    }
    T[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      T member = items[i];
      if (member == null) {
        continue;
      }
      if (!member.exists || !member.visible) {
        continue;
      }
      member.draw(batch);
    }
    members.end();
  }

  @Override
  public void remove(T member) {
    if (member == null) {
      return;
    }
    if (members == null) {
      return;
    }
    if (!members.contains(member, true)) {
      return;
    }
    member.destroy();
    members.removeValue(member, true);
  }

  @Override
  public void destroy() {
    super.destroy();
    forEachMember(FlixelBasic::destroy);
    members.clear();
    members = null;
  }

  @Override
  public void clear() {
    if (members != null) {
      members.clear();
    }
  }

  /**
   * First member reference that is non-null and {@link FlixelBasic#exists} is {@code false}, or {@code null}.
   */
  @Nullable
  public T getFirstDead() {
    if (members == null) {
      return null;
    }
    T[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      T m = items[i];
      if (m != null && !m.exists) {
        return m;
      }
    }
    members.end();
    return null;
  }

  /** Index of the first {@code null} slot in {@link #members}, or {@code -1} if none. */
  public int getFirstNullIndex() {
    if (members == null) {
      return -1;
    }
    T[] items = members.begin();
      for (int i = 0, n = members.size; i < n; i++) {
        if (items[i] == null) {
          return i;
        }
      }
    members.end();
    return -1;
  }

  /**
   * Removes {@code member} from this group without calling {@link FlixelBasic#destroy()}.
   *
   * @param member The member to detach.
   */
  public void detach(T member) {
    if (member == null || members == null || !members.contains(member, true)) {
      return;
    }
    members.removeValue(member, true);
  }

  /**
   * Removes {@code member}; when {@code destroy} is {@code true}, matches {@link #remove}; otherwise matches
   * {@link #detach}.
   *
   * @param member The member to remove.
   * @param destroy Whether to call {@link FlixelBasic#destroy()} on the member.
   */
  public void removeMember(T member, boolean destroy) {
    if (member == null) {
      return;
    }
    if (members == null || !members.contains(member, true)) {
      return;
    }
    if (destroy) {
      remove(member);
    } else {
      detach(member);
    }
  }

  /**
   * Returns a reusable member. Revives and {@link FlixelBasic#reset}s the first {@link #getFirstDead dead}
   * slot, or adds {@code factory.get()} when every slot is active. When {@link #maxSize} is exceeded,
   * returns the new instance without adding it.
   *
   * @param factory The factory to create a new member.
   * @return A reusable member.
   */
  public T recycle(@NotNull Supplier<? extends T> factory) {
    ensureMembers();
    T dead = getFirstDead();
    if (dead != null) {
      dead.revive();
      return dead;
    }
    T created = factory.get();
    if (maxSize > 0 && members.size >= maxSize) {
      return created;
    }
    members.add(created);
    return created;
  }

  /**
   * Calls {@code callback} for each member in this group. This is a safe
   * way to iterate over the members without worrying about concurrent modification, as it
   * automatically acquires a snapshot of the members array and reduces the boilerplate code
   * for you.
   *
   * @param callback The callback to call for each member.
   */
  public void forEachMember(Consumer<T> callback) {
    if (members == null) {
      return;
    }
    T[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      T member = items[i];
      if (member == null) {
        continue;
      }
      callback.accept(member);
    }
    members.end();
  }

  /**
   * Calls {@code callback} for each member in this group that is an instance of the given type.
   * This is a safe way to iterate over the members without worrying about concurrent modification, as it
   * automatically acquires a snapshot of the members array and reduces the boilerplate code for you.
   *
   * @param <C> The type of the members to iterate over.
   * @param type The type to check.
   * @param callback The callback to call for each member.
   */
  public <C> void forEachMemberType(Class<C> type, Consumer<C> callback) {
    if (members == null) {
      return;
    }
    T[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      T member = items[i];
      if (type.isInstance(member)) {
        callback.accept(type.cast(member));
      }
    }
    members.end();
  }

  @Override
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
