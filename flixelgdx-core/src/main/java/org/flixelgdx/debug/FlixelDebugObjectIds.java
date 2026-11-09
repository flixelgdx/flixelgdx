/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.debug;

import org.flixelgdx.collections.FlixelIdentityMap;
import org.flixelgdx.collections.FlixelIntArray;
import org.flixelgdx.collections.FlixelIntMap;
import org.flixelgdx.collections.FlixelIntSet;
import org.flixelgdx.debug.feeds.FlixelDebugInspector;
import org.flixelgdx.functional.FlixelExistable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Hands out small, stable numbers (ids) for the game objects that the remote debugger talks about.
 *
 * <p>The debugger app cannot hold a Java object, so it refers to one by number: "select object 42". Think of a
 * coat check. The first time an object shows up, it gets a ticket with the next free number. Showing the same
 * object again returns the same ticket, and handing in a number returns the object. Numbers only go up and are
 * never reused, so a stale number can never point at the wrong object.
 *
 * <p>Objects are compared by identity (the exact same instance), never by {@code equals}, so two objects that
 * happen to look the same still get different ids.
 *
 * <p>The registry holds its objects strongly, so an id must be given back when the object is gone. Objects that
 * implement {@link FlixelExistable} (every {@code FlixelBasic} does) are cleaned up by {@link #sweep()}. An object
 * is forgotten once it no longer exists and is no longer anywhere in the scene. Both {@code destroy()} and
 * {@code kill()} turn {@code exists} off, so {@code exists} alone cannot tell a destroyed object from one that is
 * only hidden or waiting to be recycled. The scene tells them apart instead: before sweeping, the inspector walks
 * the scene and marks every object it reaches with {@link #beginMark()} and {@link #mark(Object)}, and a sweep
 * keeps every marked object. Anything that does not implement {@link FlixelExistable} is released with
 * {@link #release(Object)}.
 *
 * <p>Assigning the id of a new object creates one small object, which happens once per object. Looking up an
 * object that already has an id does not allocate.
 *
 * <p>Example:
 *
 * <pre>{@code
 * int id = Flixel.debug.ids.idOf(player);
 * Object same = Flixel.debug.ids.get(id); // == player
 * }</pre>
 *
 * @see FlixelDebugInspector
 */
public class FlixelDebugObjectIds {

  /** The id that means "no object". */
  public static final int NONE = -1;

  private final FlixelIdentityMap<Object, Integer> ids = new FlixelIdentityMap<>();
  private final FlixelIntMap<Object> objects = new FlixelIntMap<>();
  private final FlixelIntArray dead = new FlixelIntArray();
  private final FlixelIntSet marked = new FlixelIntSet();
  private final FlixelIntSet agentTouched = new FlixelIntSet();

  private int next = 1;
  private int agentRevision;

  private boolean marking;

  /** Creates an empty registry. */
  public FlixelDebugObjectIds() {}

  /**
   * Returns the id of an object, assigning the next free one the first time the object is seen.
   *
   * @param object The object.
   * @return The id, always positive.
   */
  public int idOf(@NotNull Object object) {
    Integer existing = ids.get(object);
    if (existing != null) {
      return existing;
    }
    int id = next++;
    ids.put(object, id);
    objects.put(id, object);
    return id;
  }

  /**
   * Returns the id of an object without assigning one.
   *
   * @param object The object.
   * @return The id, or {@link #NONE} when the object has none.
   */
  public int peek(@Nullable Object object) {
    if (object == null) {
      return NONE;
    }
    Integer existing = ids.get(object);
    return existing != null ? existing : NONE;
  }

  /**
   * Returns the object that has the given id.
   *
   * @param id An id from {@link #idOf(Object)}.
   * @return The object, or {@code null} when the id is unknown or was released.
   */
  @Nullable
  public Object get(int id) {
    return objects.get(id);
  }

  /**
   * Forgets an object. Its id is never handed out again.
   *
   * @param object The object to release.
   * @return {@code true} when the object had an id.
   */
  public boolean release(@Nullable Object object) {
    if (object == null) {
      return false;
    }
    Integer id = ids.remove(object);
    if (id == null) {
      return false;
    }
    objects.remove(id);
    if (agentTouched.remove(id)) {
      agentRevision++;
    }
    return true;
  }

  /**
   * Remembers that a tool (such as an AI agent working through a feed) selected, moved, edited, or clicked an
   * object, so the panels can point it out to the person.
   *
   * @param object The object, or {@code null} to do nothing.
   */
  public void markAgentTouched(@Nullable Object object) {
    if (object != null && agentTouched.add(idOf(object))) {
      agentRevision++;
    }
  }

  /**
   * Returns whether a tool touched the object with this id.
   *
   * @param id The object's id.
   * @return {@code true} after {@link #markAgentTouched(Object)} for that object.
   */
  public boolean isAgentTouched(int id) {
    return agentTouched.contains(id);
  }

  /** Forgets which objects a tool touched. */
  public void clearAgentTouched() {
    if (!agentTouched.isEmpty()) {
      agentTouched.clear();
      agentRevision++;
    }
  }

  /**
   * Starts a new mark pass, forgetting the marks of the previous one.
   *
   * <p>Call it before marking every object that is still in the scene with {@link #mark(Object)}, then call
   * {@link #sweep()}. Once a mark pass has started, {@link #sweep()} keeps every marked object even when it no
   * longer exists. This does not allocate.
   */
  public void beginMark() {
    marked.clear();
    marking = true;
  }

  /**
   * Marks an object as still being in the scene for the current mark pass (see {@link #beginMark()}).
   *
   * <p>An object without an id is ignored, since there is nothing to keep. This does not allocate once the mark
   * set has grown to the size of the scene.
   *
   * @param object The object that was reached.
   * @return {@code true} when the object has an id and was marked.
   */
  public boolean mark(@Nullable Object object) {
    int id = peek(object);
    if (id == NONE) {
      return false;
    }
    marked.add(id);
    return true;
  }

  /**
   * Returns whether an object was marked in the latest mark pass.
   *
   * @param object The object.
   * @return {@code true} when the object has an id and was marked since the last {@link #beginMark()}.
   */
  public boolean isMarked(@Nullable Object object) {
    int id = peek(object);
    return id != NONE && marked.contains(id);
  }

  /**
   * Forgets every object that implements {@link FlixelExistable}, no longer exists, and is no longer anywhere in
   * the scene.
   *
   * <p>"In the scene" means it was marked in the latest mark pass (see {@link #beginMark()}). When no mark pass has
   * ever run, every object that no longer exists is forgotten. This walks the registry once and does not allocate.
   * The inspector marks the scene and calls this about once a second.
   *
   * @return How many objects were released.
   */
  public int sweep() {
    dead.clear();
    for (FlixelIntMap.Entry<Object> entry : objects.entries()) {
      if (entry.value instanceof FlixelExistable existable && !existable.isExists()
          && !(marking && marked.contains(entry.key))) {
        dead.add(entry.key);
      }
    }
    for (int i = 0, n = dead.getSize(); i < n; i++) {
      int id = dead.get(i);
      Object object = objects.remove(id);
      if (object != null) {
        ids.remove(object);
      }
      if (agentTouched.remove(id)) {
        agentRevision++;
      }
    }
    return dead.getSize();
  }

  /** Forgets every object. Ids already handed out are not reused afterward. */
  public void clear() {
    ids.clear();
    objects.clear();
    marked.clear();
    marking = false;
    clearAgentTouched();
  }

  /**
   * Returns how many objects currently have an id.
   *
   * @return The object count.
   */
  public int getCount() {
    return objects.getSize();
  }

  /**
   * Returns a number that goes up whenever the set of objects a tool touched changes, so panels know when to redraw
   * their marks.
   *
   * @return The revision.
   */
  public int getAgentRevision() {
    return agentRevision;
  }
}
