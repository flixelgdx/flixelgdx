package me.stringdotjar.flixelgdx.group;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.SnapshotArray;

import me.stringdotjar.flixelgdx.FlixelBasic;

import java.util.function.Consumer;

/**
 * Base class for creating groups with a list of members inside it.
 */
public abstract class FlixelGroup<T extends FlixelBasic> extends FlixelBasic implements FlixelGroupable<T> {

  /**
   * The list of members that {@code this} group contains.
   */
  protected SnapshotArray<FlixelBasic> members;

  /**
   * Maximum number of members allowed. When {@code 0}, the group can grow without limit (default).
   * When {@code > 0}, {@link #add} will not add if at capacity.
   */
  protected int maxSize = 0;

  /**
   * Creates a new FlixelGroup with no maximum size.
   */
  public FlixelGroup() {
    this(0);
  }

  /**
   * Creates a new FlixelGroup with the given maximum size.
   *
   * @param maxSize Maximum number of members allowed. When {@code 0}, the group can grow without limit (default).
   * When {@code > 0}, {@link #add} will not add if at capacity.
   */
  public FlixelGroup(int maxSize) {
    this.maxSize = Math.max(0, maxSize);
    members = new SnapshotArray<>(FlixelBasic[]::new);
  }

  @Override
  public void add(T member) {
    members.add(member);
  }

  @Override
  public void update(float elapsed) {
    FlixelBasic[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelBasic member = items[i];
      if (member == null) {
        continue;
      }
      member.update(elapsed);
    }
    members.end();
  }

  @Override
  public void draw(Batch batch) {
    FlixelBasic[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelBasic member = items[i];
      if (member == null) {
        continue;
      }
      member.draw(batch);
    }
    members.end();
  }

  @Override
  public void remove(T member) {
    members.removeValue(member, true);
  }

  @Override
  public void destroy() {
    members.forEach(FlixelBasic::destroy);
    members.clear();
  }

  @Override
  public void clear() {
    members.clear();
  }

  public void forEachMember(Consumer<FlixelBasic> callback) {
    FlixelBasic[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelBasic member = items[i];
      if (member == null) {
        continue;
      }
      callback.accept(member);
    }
    members.end();
  }

  public <C> void forEachMemberType(Class<C> type, Consumer<C> callback) {
    FlixelBasic[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelBasic member = items[i];
      if (type.isInstance(member)) {
        callback.accept(type.cast(member));
      }
    }
    members.end();
  }

  public SnapshotArray<FlixelBasic> getMembers() {
    return members;
  }
}
