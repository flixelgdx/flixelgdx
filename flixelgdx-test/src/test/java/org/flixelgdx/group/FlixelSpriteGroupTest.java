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
package org.flixelgdx.group;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.graphics.FlixelUnsupportedBatch;
import org.flixelgdx.group.FlixelSpriteGroup.RotationMode;
import org.flixelgdx.math.FlixelRect;
import org.flixelgdx.math.FlixelVector;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelDirectionFlags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelSpriteGroupTest {

  private static final float EPS = 1e-3f;

  private FlixelSpriteGroup group;

  @BeforeEach
  void setUp() {
    group = new FlixelSpriteGroup();
    group.setPosition(100f, 50f);
  }

  private static FlixelSprite sprite(float x, float y) {
    FlixelSprite s = new FlixelSprite();
    s.setPosition(x, y);
    s.setSize(10f, 10f);
    return s;
  }

  @Test
  void membersArePlacedRelativeToTheGroup() {
    FlixelSprite s = sprite(5f, 5f);
    group.add(s);
    assertEquals(105f, s.getX(), EPS);
    assertEquals(55f, s.getY(), EPS);

    group.remove(s);
    assertEquals(5f, s.getX(), EPS, "removing a member gives it back its local position");
    assertEquals(5f, s.getY(), EPS);
  }

  @Test
  void movingTheGroupMovesItsMembers() {
    FlixelSprite s = group.addAndReturn(sprite(0f, 0f));
    group.setX(110f);
    group.setY(60f);
    assertEquals(110f, s.getX(), EPS);
    assertEquals(60f, s.getY(), EPS);
    group.setPosition(0f, 0f);
    assertEquals(0f, s.getX(), EPS);
    assertEquals(0f, s.getY(), EPS);
  }

  @Test
  void wheelModeDoesNotMoveMembersWithTheGroup() {
    FlixelSprite s = group.addAndReturn(sprite(0f, 0f));
    group.setRotationMode(RotationMode.WHEEL);
    assertEquals(RotationMode.WHEEL, group.getRotationMode());
    group.setX(0f);
    group.setY(0f);
    group.setPosition(5f, 5f);
    assertEquals(100f, s.getX(), EPS);
  }

  @Test
  void wheelModeSpreadsMembersAroundACircleOnUpdate() {
    FlixelSpriteGroup wheel = new FlixelSpriteGroup(0, 10f, 0f);
    wheel.setRotationMode(RotationMode.WHEEL);
    FlixelSprite a = wheel.addAndReturn(sprite(0f, 0f));
    FlixelSprite b = wheel.addAndReturn(sprite(0f, 0f));
    wheel.setRotationRadius(20f);
    assertEquals(20f, wheel.getRotationRadius());
    wheel.setAngle(90f);
    wheel.update(0f);
    assertEquals(0f, a.getX(), EPS);
    assertEquals(20f, a.getY(), EPS);
    assertEquals(0f, b.getX(), EPS);
    assertEquals(-20f, b.getY(), EPS);
    assertEquals(270f, b.getAngle(), EPS);

    new FlixelSpriteGroup(0, 10f, 0f) {
      {
        setRotationMode(RotationMode.WHEEL);
      }
    }.update(0f);
  }

  @Test
  void individualRotationTurnsEachMember() {
    FlixelSprite s = group.addAndReturn(sprite(0f, 0f));
    group.setAngle(30f);
    assertEquals(30f, s.getAngle(), EPS);
    group.setAngle(30f);
    assertEquals(30f, s.getAngle(), EPS, "setting the same angle changes nothing");
    assertEquals(100f, s.getX(), EPS);
  }

  @Test
  void orbitRotationSwingsMembersAroundTheGroup() {
    group.setRotationMode(RotationMode.ORBIT);
    FlixelSprite s = group.addAndReturn(sprite(10f, 0f));
    group.getMembers().add(null);
    group.setAngle(90f);
    assertEquals(100f, s.getX(), EPS);
    assertEquals(60f, s.getY(), EPS);
    assertEquals(90f, s.getAngle(), EPS);
  }

  @Test
  void loadingAGraphicOnAGroupIsNotAllowed() {
    assertThrows(UnsupportedOperationException.class, () -> group.loadGraphic((FlixelTexture) null, 1, 1));
    assertThrows(UnsupportedOperationException.class, () -> group.makeGraphic(1, 1, FlixelColor.RED));
  }

  @Test
  void appearanceChangesReachEveryMember() {
    FlixelSprite s = group.addAndReturn(sprite(0f, 0f));
    group.getMembers().add(null);

    group.setAlpha(0.5f);
    assertEquals(0.5f, s.getAlpha(), EPS);
    assertEquals(0.5f, group.getAlpha(), EPS);

    group.setColor(FlixelColor.RED);
    assertEquals(0f, s.getColor().g, EPS);
    group.setColor(0f, 1f, 0f, 1f);
    assertEquals(1f, s.getColor().g, EPS);

    group.setScale(2f);
    assertEquals(2f, s.getScaleX(), EPS);
    group.setScale(3f, 4f);
    assertEquals(4f, s.getScaleY(), EPS);

    group.flip(true, false);
    assertTrue(s.isFlipX());
    group.setFlipX(false);
    assertFalse(s.isFlipX());
    group.setFlipX(false);
    group.setFlipY(true);
    assertTrue(s.isFlipY());
    group.setFlipY(true);

    group.setAntialiasing(true);
    assertTrue(group.isAntialiasing());
    assertTrue(group.getAntialiasing());
    assertTrue(s.isAntialiasing());

    group.setFacing(FlixelDirectionFlags.LEFT);
    assertEquals(FlixelDirectionFlags.LEFT, group.getFacing());
    assertEquals(FlixelDirectionFlags.LEFT, s.getFacing());

    group.setOrigin(1f, 2f);
    assertEquals(2f, s.getOriginY(), EPS);
    group.setOriginCenter();
    assertEquals(5f, s.getOriginX(), EPS);
  }

  @Test
  void newMembersTakeTheGroupsAppearance() {
    group.setAlpha(0.5f);
    group.setAntialiasing(true);
    group.setFacing(FlixelDirectionFlags.LEFT);
    FlixelSprite s = sprite(0f, 0f);
    s.setAlpha(0.5f);
    group.add(s);
    assertEquals(0.25f, s.getAlpha(), EPS);
    assertTrue(s.isAntialiasing());
    assertEquals(FlixelDirectionFlags.LEFT, s.getFacing());
  }

  @Test
  void maxSizeLimitsAddInsertAndRecycle() {
    FlixelSpriteGroup g = new FlixelSpriteGroup(1, 100f, 0f);
    g.add(sprite(0f, 0f));
    g.add(sprite(0f, 0f));
    g.insert(0, sprite(0f, 0f));
    assertNull(g.recycle());
    assertEquals(1, g.getLength());
    assertEquals(1, g.getMaxSize());
    g.setMaxSize(-1);
    assertEquals(0, g.getMaxSize());
    assertThrows(NullPointerException.class, () -> g.add(null));
    assertThrows(NullPointerException.class, () -> g.addAndReturn(null));
  }

  @Test
  void insertPlacesTheMemberAtAClampedIndex() {
    FlixelSprite a = group.addAndReturn(sprite(0f, 0f));
    FlixelSprite b = sprite(0f, 0f);
    FlixelSprite c = sprite(0f, 0f);
    group.insert(-5, b);
    group.insert(99, c);
    group.insert(0, null);
    assertSame(b, group.get(0));
    assertSame(a, group.get(1));
    assertSame(c, group.get(2));
    assertEquals(100f, b.getX(), EPS);
  }

  @Test
  void recycleRevivesDeadMembersOrMakesNewOnes() {
    FlixelSprite fresh = group.recycle();
    assertNotNull(fresh);
    assertEquals(1, group.getLength());
    fresh.kill();
    assertSame(fresh, group.recycle());
    assertTrue(fresh.isExists());
    assertTrue(fresh.isVisible());
  }

  @Test
  void replaceSwapsAMemberInPlace() {
    FlixelSprite a = group.addAndReturn(sprite(0f, 0f));
    FlixelSprite b = sprite(0f, 0f);
    assertSame(b, group.replace(a, b));
    assertSame(b, group.get(0));
    assertEquals(0f, a.getX(), EPS);
    assertEquals(100f, b.getX(), EPS);

    FlixelSprite c = sprite(0f, 0f);
    assertSame(c, group.replace(sprite(0f, 0f), c), "replacing a stranger adds the new sprite");
    assertEquals(2, group.getLength());
    assertSame(c, group.replace(null, c));
    assertNull(group.replace(a, null));
  }

  @Test
  void lookupHelpers() {
    assertTrue(group.isEmpty());
    assertTrue(group.getEmpty());
    assertNull(group.getRandom());
    FlixelSprite a = group.addAndReturn(sprite(0f, 0f));
    FlixelSprite b = group.addAndReturn(sprite(1f, 0f));
    assertNotNull(group.getRandom(0, 0), "a length of zero or less means the rest of the group");
    group.getMembers().add(null);
    assertFalse(group.isEmpty());
    assertEquals(3, group.getLength());
    assertEquals(2, group.countMembers());
    assertNull(group.get(-1));
    assertNull(group.get(10));
    assertEquals(1, group.indexOf(b));
    assertTrue(group.contains(a));
    assertFalse(group.contains(sprite(0f, 0f)));
    assertSame(b, group.getFirstMatching(s -> s.getX() > 100.5f));
    assertNull(group.getFirstMatching(null));
    assertNull(group.getFirstMatching(s -> false));

    FlixelSprite r = group.getRandom(1, 1);
    assertSame(b, r);
    assertNull(group.getRandom(5, 1), "the start is clamped to the last slot, which is empty");
  }

  @Test
  void orderHelpers() {
    FlixelSprite a = group.addAndReturn(sprite(0f, 0f));
    FlixelSprite b = group.addAndReturn(sprite(0f, 0f));
    FlixelSprite c = group.addAndReturn(sprite(0f, 0f));
    group.bringToFront(a);
    assertSame(a, group.get(2));
    group.sendToBack(c);
    assertSame(c, group.get(0));
    group.bringToFront(sprite(0f, 0f));
    group.sendToBack(sprite(0f, 0f));
    group.swapMembers(0, 2);
    assertSame(a, group.get(0));
    group.swapMembers(-1, 0);
    group.swapMembers(0, 9);
    group.swapMembers(9, 0);
    group.swapMembers(0, -1);

    a.setY(300f);
    b.setY(100f);
    c.setY(200f);
    group.sort(Comparator.comparingDouble(FlixelSprite::getY));
    assertSame(b, group.get(0));
    assertSame(a, group.get(2));
    assertThrows(NullPointerException.class, () -> group.sort(null));
  }

  @Test
  void boundsCoverEveryMember() {
    FlixelRect empty = group.getBounds(null);
    assertEquals(100f, empty.x, EPS);
    assertEquals(0f, empty.width, EPS);

    group.add(sprite(0f, 0f));
    FlixelSprite big = sprite(20f, 30f);
    big.setScale(-2f, 2f);
    group.add(big);
    group.getMembers().add(null);
    FlixelRect r = group.getBounds(new FlixelRect());
    assertEquals(100f, r.x, EPS);
    assertEquals(50f, r.y, EPS);
    assertEquals(40f, r.width, EPS);
    assertEquals(50f, r.height, EPS);

    FlixelVector mid = group.getMidpoint(null);
    assertEquals(120f, mid.x, EPS);
    assertEquals(75f, mid.y, EPS);
    assertSame(mid, group.getMidpoint(mid));
  }

  @Test
  void updateAndDrawReachEveryMember() {
    Flixel.cameras.add(new FlixelCamera(320, 240));
    FlixelSprite s = group.addAndReturn(sprite(0f, 0f));
    s.setVelocity(10f, 0f);
    group.getMembers().add(null);
    group.update(1f);
    assertEquals(110f, s.getX(), 0.5f);
    group.draw(FlixelUnsupportedBatch.INSTANCE);

    group.clear();
    group.draw(FlixelUnsupportedBatch.INSTANCE);
    group.setVisible(false);
    group.draw(FlixelUnsupportedBatch.INSTANCE);
  }

  @Test
  void clearDetachAndDestroyGiveMembersTheirLocalPositions() {
    FlixelSprite a = group.addAndReturn(sprite(1f, 1f));
    FlixelSprite b = group.addAndReturn(sprite(2f, 2f));
    group.getMembers().add(null);
    group.detach(a);
    group.detach(a);
    group.detach(null);
    group.remove(null);
    assertEquals(1f, a.getX(), EPS);
    group.clear();
    assertEquals(2f, b.getX(), EPS);

    FlixelSprite c = group.addAndReturn(sprite(3f, 3f));
    group.getMembers().add(null);
    group.setRotationMode(RotationMode.ORBIT);
    group.setRotationRadius(5f);
    group.destroy();
    assertFalse(group.contains(c));
    assertEquals(0, group.getLength());
    assertEquals(RotationMode.INDIVIDUAL, group.getRotationMode());
    assertEquals(100f, group.getRotationRadius());
    assertTrue(group.isVisible());
  }

  @Test
  void findingAMemberDoesNotLeaveTheMemberListInSnapshotMode() {
    FlixelSprite a = group.addAndReturn(sprite(0f, 0f));
    FlixelSprite b = group.addAndReturn(sprite(1f, 0f));
    assertSame(a, group.getFirstMatching(s -> true));
    FlixelSprite[] before = group.getMembers().getItems();
    group.remove(b);
    assertSame(before, group.getMembers().getItems(), "a remove outside a loop must not copy the backing array");
  }
}
