/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.group;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.ArraySupplier;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.SnapshotArray;

import me.stringdotjar.flixelgdx.FlixelBasic;
import me.stringdotjar.flixelgdx.FlixelSprite;

import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for groups with a {@link SnapshotArray} of members and a mandatory {@link Pool}.
 *
 * <p>{@link #remove} detaches the member and {@link Pool#free}s it. Pool {@code reset} runs {@link FlixelBasic#destroy}.
 * {@link #detach} only removes from the list (for reparenting). Use {@link #recycle()} or {@link #obtainMember()} so
 * instances come from the same pool.
 *
 * <p>Factory: {@link #createSpriteMemberPool(int)} builds a {@link Pool} of {@link FlixelSprite} instances as
 * {@link FlixelBasic} for typical {@link me.stringdotjar.flixelgdx.FlixelState} usage.
 */
public abstract class FlixelGroup<T extends FlixelBasic> extends FlixelBasic implements FlixelBasicGroupable<T> {

  protected SnapshotArray<T> members;

  private final ArraySupplier<T[]> memberArrayFactory;

  protected int maxSize = 0;

  @NotNull
  protected final Pool<T> memberPool;

  /**
   * Pool that allocates {@link FlixelSprite} instances as {@link FlixelBasic} (common default for {@link me.stringdotjar.flixelgdx.FlixelState}).
   */
  @NotNull
  public static Pool<FlixelBasic> createSpriteMemberPool(int initialCapacity) {
    int cap = Math.max(1, initialCapacity);
    return new Pool<FlixelBasic>(cap) {
      @Override
      protected FlixelBasic newObject() {
        return new FlixelSprite();
      }
    };
  }

  protected FlixelGroup(@NotNull ArraySupplier<T[]> arrayFactory, @NotNull Pool<T> memberPool) {
    this(arrayFactory, 0, memberPool);
  }

  protected FlixelGroup(@NotNull ArraySupplier<T[]> arrayFactory, int maxSize, @NotNull Pool<T> memberPool) {
    this.memberArrayFactory = Objects.requireNonNull(arrayFactory, "Array factory cannot be null");
    this.memberPool = Objects.requireNonNull(memberPool, "Member pool cannot be null");
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
  public void update(float elapsed) {
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
        if (!member.exists || !member.active) {
          continue;
        }
        member.update(elapsed);
      }
    } finally {
      members.end();
    }
  }

  @Override
  public void draw(Batch batch) {
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
        if (!member.exists || !member.visible) {
          continue;
        }
        member.draw(batch);
      }
    } finally {
      members.end();
    }
  }

  @Override
  public void remove(T member) {
    if (member == null || members == null) {
      return;
    }
    if (!members.removeValue(member, true)) {
      return;
    }
    memberPool.free(member);
  }

  @Override
  public void destroy() {
    super.destroy();
    if (members != null) {
      try {
        T[] items = members.begin();
        for (int i = 0, n = members.size; i < n; i++) {
          T m = items[i];
          if (m != null) {
            memberPool.free(m);
          }
        }
      } finally {
        members.end();
      }
      members.clear();
      members = null;
    }
  }

  @Override
  public void clear() {
    if (members == null) {
      return;
    }
    try {
      T[] items = members.begin();
      for (int i = 0, n = members.size; i < n; i++) {
        T m = items[i];
        if (m != null) {
          memberPool.free(m);
        }
      }
    } finally {
      members.end();
    }
    members.clear();
  }

  @Nullable
  public T getFirstDead() {
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

  /** Index of the first {@code null} slot in {@link #members}, or {@code -1} if none. */
  public int getFirstNullIndex() {
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
   * Removes the member from the group without destroying it.
   *
   * @param member The member to remove.
   */
  public void detach(T member) {
    if (member == null || members == null) {
      return;
    }
    members.removeValue(member, true);
  }

  /**
   * Removes the member from the group and optionally destroys it.
   *
   * @param member The member to remove.
   * @param destroy Whether to destroy the member.
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
  public T recycle() {
    ensureMembers();
    T dead = getFirstDead();
    if (dead != null) {
      dead.revive();
      dead.active = true;
      dead.visible = true;
      return dead;
    }
    T pooled = memberPool.obtain();
    pooled.revive();
    pooled.active = true;
    pooled.visible = true;
    if (maxSize > 0 && members.size >= maxSize) {
      memberPool.free(pooled);
      return pooled;
    }
    members.add(pooled);
    return pooled;
  }

  /**
   * Calls {@code callback} for each member in {@code this} group. This is a safe
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
   * Calls {@code callback} for each member in {@code this} group that is an instance of the given type.
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

  @Override
  @NotNull
  public Pool<T> getMemberPool() {
    return memberPool;
  }
}
