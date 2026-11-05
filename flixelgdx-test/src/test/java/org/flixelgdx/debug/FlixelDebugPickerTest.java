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

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.FlixelState;
import org.flixelgdx.FlixelSubState;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelNoopTexture;
import org.flixelgdx.group.FlixelSpriteGroup;
import org.flixelgdx.math.FlixelVector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Picking works in a camera's view space, so these tests give points in view space directly. */
class FlixelDebugPickerTest {

  private FlixelDebugPicker picker;
  private FlixelCamera cam;
  private FlixelState state;

  @BeforeEach
  void setUp() {
    picker = new FlixelDebugPicker();
    state = new TestState();
    Flixel.state = state;
    Flixel.cameras.clear();
    cam = new FlixelCamera(400, 300);
    cam.scrollX = 0f;
    cam.scrollY = 0f;
    Flixel.cameras.add(cam);
  }

  @AfterEach
  void tearDown() {
    Flixel.state = null;
    Flixel.cameras.clear();
  }

  @Test
  void theObjectDrawnLastIsOnTop() {
    FlixelObject below = box(10, 10, 100, 100);
    FlixelObject above = box(50, 50, 100, 100);
    state.add(below);
    state.add(above);

    assertSame(above, picker.pickInCamera(cam, 60f, 60f), "inside both");
    assertSame(below, picker.pickInCamera(cam, 20f, 20f), "inside only the lower one");
    assertNull(picker.pickInCamera(cam, 300f, 250f));
  }

  @Test
  void invisibleKilledAndMissingObjectsAreSkipped() {
    FlixelObject below = box(0, 0, 100, 100);
    FlixelObject hidden = box(0, 0, 100, 100);
    FlixelObject dead = box(0, 0, 100, 100);
    FlixelObject gone = box(0, 0, 100, 100);
    state.add(below);
    state.add(hidden);
    state.add(dead);
    state.add(gone);
    hidden.visible = false;
    dead.kill();
    gone.exists = false;

    assertSame(below, picker.pickInCamera(cam, 5f, 5f));

    hidden.visible = true;
    assertSame(hidden, picker.pickInCamera(cam, 5f, 5f));
  }

  @Test
  void groupsAreSearchedMemberByMemberAndHideTheirMembersWhenHidden() {
    FlixelSpriteGroup group = new FlixelSpriteGroup();
    FlixelSprite inside = new FlixelSprite(10f, 10f);
    inside.setSize(40f, 40f);
    FlixelObject behind = box(0, 0, 200, 200);
    group.add(inside);
    state.add(behind);
    state.add(group);

    assertSame(inside, picker.pickInCamera(cam, 20f, 20f));

    group.visible = false;
    assertSame(behind, picker.pickInCamera(cam, 20f, 20f));
    group.visible = true;
    group.exists = false;
    assertSame(behind, picker.pickInCamera(cam, 20f, 20f));
  }

  @Test
  void theCameraScrollMovesTheHitArea() {
    FlixelObject object = box(500, 400, 50, 50);
    state.add(object);
    assertNull(picker.pickInCamera(cam, 10f, 10f));

    cam.scrollX = 490f;
    cam.scrollY = 390f;

    assertSame(object, picker.pickInCamera(cam, 10f, 10f));
    assertNull(picker.pickInCamera(cam, 80f, 80f));
  }

  @Test
  void aScrollFactorMakesTheObjectFollowTheCameraPartOfTheWay() {
    FlixelObject far = box(200, 100, 40, 40);
    far.setScrollFactor(0.5f, 0.5f);
    state.add(far);
    cam.scrollX = 100f;
    cam.scrollY = 0f;

    // The camera moved 100, the object only 50, so it sits at 150 in view space.
    assertSame(far, picker.pickInCamera(cam, 160f, 110f));
    assertNull(picker.pickInCamera(cam, 110f, 110f));

    FlixelObject hud = box(0, 0, 40, 40);
    hud.setScrollFactor(0f, 0f);
    state.add(hud);
    assertSame(hud, picker.pickInCamera(cam, 10f, 10f), "a fixed object stays where it is drawn");
  }

  @Test
  void zoomShrinksTheVisibleAreaAndShiftsTheViewMargin() {
    cam.setZoom(2f);
    // At zoom 2 the camera shows 200 x 150 world units, so the view margin is 100 and 75.
    FlixelObject object = box(110, 80, 20, 20);
    state.add(object);

    assertSame(object, picker.pickInCamera(cam, 15f, 15f));
    assertNull(picker.pickInCamera(cam, 40f, 15f));
    assertNull(picker.pickInCamera(cam, 250f, 15f), "outside the zoomed view");
  }

  @Test
  void aSpriteIsHitWhereItsGraphicIsDrawnIncludingOffsetAndScale() {
    FlixelSprite sprite = new FlixelSprite(10f, 10f);
    sprite.setRegion(new FlixelFrame(new FlixelNoopTexture(32, 16)));
    sprite.setSize(4f, 4f);
    sprite.setOffset(5f, 3f);
    sprite.setScale(2f);
    state.add(sprite);

    // The graphic is 64 x 32 on screen and starts at (10 + 5, 10 + 3).
    assertSame(sprite, picker.pickInCamera(cam, 60f, 40f));
    assertSame(sprite, picker.pickInCamera(cam, 16f, 14f));
    assertNull(picker.pickInCamera(cam, 14f, 14f), "left of the offset graphic");
    assertNull(picker.pickInCamera(cam, 80f, 40f), "right of the scaled graphic");
    assertTrue(picker.computeBounds(sprite, cam));
    assertEquals(15f, picker.getBoundsLeft(), 1e-4f);
    assertEquals(13f, picker.getBoundsTop(), 1e-4f);
    assertEquals(64f, picker.getBoundsWidth(), 1e-4f);
    assertEquals(32f, picker.getBoundsHeight(), 1e-4f);
  }

  @Test
  void aRotatedSpriteUsesItsBoundingBox() {
    FlixelSprite sprite = new FlixelSprite(100f, 100f);
    sprite.setRegion(new FlixelFrame(new FlixelNoopTexture(40, 20)));
    sprite.setAngle(90f);

    assertTrue(picker.computeBounds(sprite, cam));
    assertEquals(20f, picker.getBoundsWidth(), 1e-3f);
    assertEquals(40f, picker.getBoundsHeight(), 1e-3f);
    // Rotating around the center keeps the center at (120, 110).
    assertEquals(110f, picker.getBoundsLeft(), 1e-3f);
    assertEquals(90f, picker.getBoundsTop(), 1e-3f);
  }

  @Test
  void objectsOnlyCountOnTheCamerasTheyAreDrawnOn() {
    FlixelCamera second = new FlixelCamera(400, 300);
    Flixel.cameras.add(second);
    FlixelObject object = box(0, 0, 50, 50);
    object.cameras = new FlixelCamera[] { second };
    state.add(object);

    assertNull(picker.pickInCamera(cam, 10f, 10f));
    assertSame(object, picker.pickInCamera(second, 10f, 10f));
    assertTrue(FlixelDebugPicker.isDrawnOn(object, second));
    assertFalse(FlixelDebugPicker.isDrawnOn(object, cam));

    object.cameras = null;
    second.defaultDrawTarget = false;
    assertSame(object, picker.pickInCamera(cam, 10f, 10f));
    assertNull(picker.pickInCamera(second, 10f, 10f));
  }

  @Test
  void anOpenSubStateIsOnTopAndTheStateBelowOnlyCountsWhileItStillDraws() {
    FlixelObject lower = box(0, 0, 100, 100);
    state.add(lower);
    TestSubState sub = new TestSubState();
    FlixelObject upper = box(0, 0, 50, 50);
    sub.add(upper);
    state.openSubState(sub);

    assertSame(upper, picker.pickInCamera(cam, 10f, 10f));
    assertSame(lower, picker.pickInCamera(cam, 80f, 80f));

    state.persistentDraw = false;
    assertNull(picker.pickInCamera(cam, 80f, 80f));
  }

  @Test
  void pointsOutsideTheCameraViewFindNothing() {
    state.add(box(-50, -50, 1000, 1000));
    assertNull(picker.pickInCamera(cam, -1f, 10f));
    assertNull(picker.pickInCamera(cam, 10f, -1f));
    assertNull(picker.pickInCamera(cam, 401f, 10f));
    assertSame(state.getMembers().get(0), picker.pickInCamera(cam, 399f, 299f));
  }

  @Test
  void aScreenPointIsUnprojectedThroughTheCameraThenPicked() {
    FlixelObject object = box(100, 100, 50, 50);
    state.add(object);
    cam.getViewport().setScreenBounds(0, 0, 400, 300);
    cam.getViewport().setCameraPosition(200f, 150f);

    FlixelVector screen = new FlixelVector(120f, 120f);
    cam.getViewport().project(screen);

    assertSame(object, picker.pickAt(screen.x, screen.y));
    assertNull(picker.pickAt(screen.x + 200f, screen.y));
  }

  private static FlixelObject box(float x, float y, float width, float height) {
    return new FlixelObject(x, y, width, height);
  }

  private static final class TestState extends FlixelState {
  }

  private static final class TestSubState extends FlixelSubState {
  }
}
