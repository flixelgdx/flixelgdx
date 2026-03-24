package me.stringdotjar.flixelgdx.group;

import com.badlogic.gdx.utils.SnapshotArray;

import me.stringdotjar.flixelgdx.FlixelBasic;

/**
 * Interface for group-like containers that hold a list of members. Both
 * {@link FlixelGroup} and {@link FlixelSpriteGroup} implement this, allowing
 * generic traversal of the object tree (e.g. for debug utilities).
 *
 * @param <T> The member type (must extend {@link FlixelBasic}).
 */
public interface FlixelGroupable<T extends FlixelBasic> {

  /** Adds a member to this group. */
  void add(T member);

  /** Removes a member from this group. */
  void remove(T member);

  /** Removes all members from this group. */
  void clear();

  /** Returns the backing array of members. */
  SnapshotArray<? extends FlixelBasic> getMembers();

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
