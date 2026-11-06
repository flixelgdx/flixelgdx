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

import org.flixelgdx.FlixelSprite;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugObjectIdsTest {

  @Test
  void idsAreStableIncreasingAndLookUpBothWays() {
    FlixelDebugObjectIds ids = new FlixelDebugObjectIds();
    Object a = new Object();
    Object b = new Object();
    int idA = ids.idOf(a);
    int idB = ids.idOf(b);
    assertTrue(idA > 0);
    assertTrue(idB > idA);
    assertEquals(idA, ids.idOf(a));
    assertEquals(idB, ids.idOf(b));
    assertSame(a, ids.get(idA));
    assertSame(b, ids.get(idB));
    assertEquals(idA, ids.peek(a));
    assertEquals(2, ids.getCount());
  }

  @Test
  void peekDoesNotAssignAndUnknownIdsReturnNull() {
    FlixelDebugObjectIds ids = new FlixelDebugObjectIds();
    assertEquals(FlixelDebugObjectIds.NONE, ids.peek(new Object()));
    assertEquals(FlixelDebugObjectIds.NONE, ids.peek(null));
    assertNull(ids.get(12345));
    assertEquals(0, ids.getCount());
  }

  @Test
  void objectsAreComparedByIdentityNotEquals() {
    FlixelDebugObjectIds ids = new FlixelDebugObjectIds();
    String first = new String("same");
    String second = new String("same");
    assertNotEquals(ids.idOf(first), ids.idOf(second));
  }

  @Test
  void releasedIdsAreNotReused() {
    FlixelDebugObjectIds ids = new FlixelDebugObjectIds();
    Object a = new Object();
    int idA = ids.idOf(a);
    assertTrue(ids.release(a));
    assertFalse(ids.release(a));
    assertNull(ids.get(idA));
    assertTrue(ids.idOf(a) > idA);
  }

  @Test
  void sweepForgetsOnlyObjectsThatNoLongerExist() {
    FlixelDebugObjectIds ids = new FlixelDebugObjectIds();
    FlixelSprite alive = new FlixelSprite();
    FlixelSprite destroyed = new FlixelSprite();
    FlixelSprite killed = new FlixelSprite();
    Object plain = new Object();
    int aliveId = ids.idOf(alive);
    int destroyedId = ids.idOf(destroyed);
    int killedId = ids.idOf(killed);
    int plainId = ids.idOf(plain);

    destroyed.destroy();
    killed.kill();

    assertEquals(2, ids.sweep());
    assertSame(alive, ids.get(aliveId));
    assertSame(plain, ids.get(plainId));
    assertNull(ids.get(destroyedId));
    assertNull(ids.get(killedId));
    assertEquals(FlixelDebugObjectIds.NONE, ids.peek(destroyed));
    assertEquals(2, ids.getCount());
    assertEquals(0, ids.sweep());
  }

  @Test
  void sweepKeepsObjectsMarkedInTheLatestPass() {
    FlixelDebugObjectIds ids = new FlixelDebugObjectIds();
    FlixelSprite inScene = new FlixelSprite();
    FlixelSprite removed = new FlixelSprite();
    FlixelSprite unknown = new FlixelSprite();
    int inSceneId = ids.idOf(inScene);
    int removedId = ids.idOf(removed);
    inScene.kill();
    removed.kill();

    ids.beginMark();
    assertTrue(ids.mark(inScene));
    assertFalse(ids.mark(unknown), "an object without an id is not marked");
    assertTrue(ids.isMarked(inScene));
    assertFalse(ids.isMarked(removed));

    assertEquals(1, ids.sweep());
    assertSame(inScene, ids.get(inSceneId));
    assertNull(ids.get(removedId));

    ids.beginMark();
    assertFalse(ids.isMarked(inScene), "a new pass starts with no marks");
    assertEquals(1, ids.sweep());
    assertNull(ids.get(inSceneId));
  }

  @Test
  void clearForgetsEverythingWithoutReusingIds() {
    FlixelDebugObjectIds ids = new FlixelDebugObjectIds();
    Object a = new Object();
    int idA = ids.idOf(a);
    ids.clear();
    assertEquals(0, ids.getCount());
    assertNull(ids.get(idA));
    assertTrue(ids.idOf(a) > idA);
  }
}
