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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link FlixelBasic} that owns a {@link FlixelGroup} of other {@link FlixelBasic} members, with batch
 * {@link #update(float)} / {@link #draw(Batch)}, {@link #recycle()}, and {@link #destroy()} that tears down members.
 *
 * <p>Member list operations are delegated to an internal {@link FlixelGroup}; {@link #getMemberList()} exposes it when
 * you need the raw container.
 *
 * <p>{@link #remove} and {@link #detach} only unlink members; they do not call {@link FlixelBasic#destroy()}. Prefer
 * {@link FlixelBasic#kill()} / {@link #recycle()} for reuse. See {@link FlixelBasic} for lifecycle guidance.
 *
 * @param <T> Member type.
 * @see FlixelGroup
 * @see me.stringdotjar.flixelgdx.FlixelState
 */
public abstract class FlixelBasicGroup<T extends FlixelBasic> extends FlixelBasic implements FlixelBasicGroupable<T> {

  private final FlixelGroup<T> memberList;

  protected FlixelBasicGroup(@NotNull ArraySupplier<T[]> arrayFactory) {
    this(arrayFactory, 0);
  }

  protected FlixelBasicGroup(@NotNull ArraySupplier<T[]> arrayFactory, int maxSize) {
    memberList = new FlixelGroup<>(arrayFactory, maxSize);
  }

  /**
   * The backing {@link FlixelGroup} used for storage. Use for advanced access; most code should call {@link #add},
   * {@link #getMembers}, etc. on this group.
   */
  protected @NotNull FlixelGroup<T> getMemberList() {
    return memberList;
  }

  /**
   * Called by {@link #recycle()} when no dead member exists and the group is under {@link #getMaxSize()}. The default
   * returns {@code null}. {@link me.stringdotjar.flixelgdx.FlixelState} overrides this to allocate a
   * {@link me.stringdotjar.flixelgdx.FlixelSprite}.
   */
  protected @Nullable T createMemberForRecycle() {
    return null;
  }

  public void ensureMembers() {
    memberList.ensureMembers();
  }

  @Override
  public void add(T member) {
    memberList.add(member);
  }

  @Override
  public void remove(T member) {
    memberList.remove(member);
  }

  @Override
  public void clear() {
    memberList.clear();
  }

  @Override
  public void update(float elapsed) {
    SnapshotArray<T> members = memberList.getMembers();
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
    SnapshotArray<T> members = memberList.getMembers();
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
  public void destroy() {
    super.destroy();
    SnapshotArray<T> members = memberList.getMembers();
    if (members != null) {
      try {
        T[] items = members.begin();
        for (int i = 0, n = members.size; i < n; i++) {
          T m = items[i];
          if (m != null) {
            m.destroy();
          }
        }
      } finally {
        members.end();
      }
      members.clear();
    }
    memberList.resetStorage();
  }

  /**
   * Revives the first {@link #getFirstDead() dead} member, or uses {@link #createMemberForRecycle()}, revives it, adds it
   * when under max size, and returns it.
   *
   * @return A member ready to use, or {@code null} if nothing could be recycled or created.
   */
  public @Nullable T recycle() {
    memberList.ensureMembers();
    T dead = getFirstDead();
    if (dead != null) {
      dead.revive();
      dead.active = true;
      dead.visible = true;
      return dead;
    }
    if (memberList.getMaxSize() > 0 && memberList.getMembers() != null && memberList.getMembers().size >= memberList.getMaxSize()) {
      return null;
    }
    T fresh = createMemberForRecycle();
    if (fresh == null) {
      return null;
    }
    fresh.revive();
    fresh.active = true;
    fresh.visible = true;
    memberList.add(fresh);
    return fresh;
  }

  @Override
  @Nullable
  public SnapshotArray<T> getMembers() {
    return memberList.getMembers();
  }

  @Override
  public int getMaxSize() {
    return memberList.getMaxSize();
  }

  @Override
  public void setMaxSize(int maxSize) {
    memberList.setMaxSize(maxSize);
  }
}
