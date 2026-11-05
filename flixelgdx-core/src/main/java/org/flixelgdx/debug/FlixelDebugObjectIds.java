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
 * implement {@link FlixelExistable} (every {@code FlixelBasic} does) are cleaned up by {@link #sweep()} once they
 * stop existing (after {@code destroy()} or {@code kill()}). Anything else is released with
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
 * @see FlixelDebugInspectorPanel
 */
public class FlixelDebugObjectIds {

  /** The id that means "no object". */
  public static final int NONE = -1;

  private final FlixelIdentityMap<Object, Integer> ids = new FlixelIdentityMap<>();
  private final FlixelIntMap<Object> objects = new FlixelIntMap<>();
  private final FlixelIntArray dead = new FlixelIntArray();

  private int next = 1;

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
    return true;
  }

  /**
   * Forgets every object that implements {@link FlixelExistable} and no longer exists.
   *
   * <p>This walks the registry once and does not allocate. The inspector calls it about once a second.
   *
   * @return How many objects were released.
   */
  public int sweep() {
    dead.clear();
    for (FlixelIntMap.Entry<Object> entry : objects.entries()) {
      if (entry.value instanceof FlixelExistable existable && !existable.isExists()) {
        dead.add(entry.key);
      }
    }
    for (int i = 0, n = dead.getSize(); i < n; i++) {
      int id = dead.get(i);
      Object object = objects.remove(id);
      if (object != null) {
        ids.remove(object);
      }
    }
    return dead.getSize();
  }

  /** Forgets every object. Ids already handed out are not reused afterward. */
  public void clear() {
    ids.clear();
    objects.clear();
  }

  /**
   * Returns how many objects currently have an id.
   *
   * @return The object count.
   */
  public int getCount() {
    return objects.getSize();
  }
}
