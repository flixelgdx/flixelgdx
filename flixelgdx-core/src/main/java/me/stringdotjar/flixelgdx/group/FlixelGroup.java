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

/**
 * Base class for creating groups with a list of members inside it.
 */
public abstract class FlixelGroup<T extends FlixelBasic> extends FlixelBasic implements FlixelBasicGroupable<T> {

  /**
   * The list of members that {@code this} group contains.
   */
  protected SnapshotArray<T> members;

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
    this.maxSize = Math.max(0, maxSize);
    members = new SnapshotArray<>(arrayFactory);
  }

  @Override
  public void update(float elapsed) {
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
    if (!members.contains(member, true)) {
      return;
    }
    member.destroy();
    members.removeValue(member, true);
  }

  @Override
  public void destroy() {
    super.destroy();
    members.forEach(FlixelBasic::destroy);
    members.clear();
  }

  @Override
  public void clear() {
    members.clear();
  }

  public void forEachMember(Consumer<T> callback) {
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

  public <C> void forEachMemberType(Class<C> type, Consumer<C> callback) {
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
