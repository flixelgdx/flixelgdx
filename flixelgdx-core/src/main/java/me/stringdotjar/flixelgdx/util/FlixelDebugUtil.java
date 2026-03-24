package me.stringdotjar.flixelgdx.util;

import com.badlogic.gdx.utils.SnapshotArray;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelBasic;
import me.stringdotjar.flixelgdx.box2d.FlixelBox2DObject;
import me.stringdotjar.flixelgdx.debug.FlixelDebugDrawable;
import me.stringdotjar.flixelgdx.display.FlixelState;
import me.stringdotjar.flixelgdx.group.FlixelGroupable;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

/**
 * Utility methods used by the debug overlay for recursively traversing the state's object
 * tree (counting active members, iterating {@link FlixelDebugDrawable} instances for
 * bounding-box drawing, syncing Box2D objects, etc.).
 *
 * <p>Recursion descends into any member that implements {@link FlixelGroupable}, which
 * covers both {@link me.stringdotjar.flixelgdx.group.FlixelGroup} and
 * {@link me.stringdotjar.flixelgdx.group.FlixelSpriteGroup}.
 */
public final class FlixelDebugUtil {

  private FlixelDebugUtil() {}

  /**
   * Recursively counts all active members in the current state's object tree. A member is
   * counted when {@code exists == true}.
   *
   * @return The number of active members, or {@code 0} if no state is loaded.
   */
  public static int countActiveMembers() {
    FlixelState state = Flixel.getState();
    if (state == null) {
      return 0;
    }
    return countActiveMembersRecursive(state.getMembers());
  }

  private static int countActiveMembersRecursive(SnapshotArray<? extends FlixelBasic> members) {
    int count = 0;
    Object[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelBasic member = (FlixelBasic) items[i];
      if (member == null) {
        continue;
      }
      if (member.exists) {
        count++;
      }
      if (member instanceof FlixelGroupable<?> group) {
        count += countActiveMembersRecursive(group.getMembers());
      }
    }
    members.end();
    return count;
  }

  /**
   * Iterates all visible {@link FlixelDebugDrawable} instances in the current state's
   * object tree (where {@code exists} and {@code visible} are both {@code true}),
   * invoking the callback for each one. No intermediate collection is created.
   *
   * @param callback Invoked once per visible {@link FlixelDebugDrawable}.
   */
  public static void forEachDebugDrawable(Consumer<FlixelDebugDrawable> callback) {
    FlixelState state = Flixel.getState();
    if (state == null) {
      return;
    }
    forEachDebugDrawableRecursive(state.getMembers(), callback);
  }

  private static void forEachDebugDrawableRecursive(@NotNull SnapshotArray<? extends FlixelBasic> members,
                                                    @NotNull Consumer<FlixelDebugDrawable> callback) {
    Object[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelBasic member = (FlixelBasic) items[i];
      if (member == null) {
        continue;
      }
      if (member instanceof FlixelDebugDrawable drawable && member.exists) {
        callback.accept(drawable);
      }
      if (member instanceof FlixelGroupable<?> group) {
        forEachDebugDrawableRecursive(group.getMembers(), callback);
      }
    }
    members.end();
  }

  /**
   * Iterates all active {@link FlixelBox2DObject} instances in the current state's
   * object tree (where the underlying {@link FlixelBasic#exists} is {@code true}),
   * invoking the callback for each one that has a non-null body.
   *
   * @param callback Invoked once per active {@link FlixelBox2DObject} with a body.
   */
  public static void forEachBox2DObject(Consumer<FlixelBox2DObject> callback) {
    FlixelState state = Flixel.getState();
    if (state == null) {
      return;
    }
    forEachBox2DObjectRecursive(state.getMembers(), callback);
  }

  private static void forEachBox2DObjectRecursive(@NotNull SnapshotArray<? extends FlixelBasic> members,
                                                  @NotNull Consumer<FlixelBox2DObject> callback) {
    Object[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelBasic member = (FlixelBasic) items[i];
      if (member == null) {
        continue;
      }
      if (member instanceof FlixelBox2DObject box2d && member.exists && box2d.getBody() != null) {
        callback.accept(box2d);
      }
      if (member instanceof FlixelGroupable<?> group) {
        forEachBox2DObjectRecursive(group.getMembers(), callback);
      }
    }
    members.end();
  }
}
