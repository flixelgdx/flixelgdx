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
import org.flixelgdx.FlixelCamera.FollowStyle;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.FlixelState;
import org.flixelgdx.FlixelSubState;
import org.flixelgdx.debug.panels.FlixelDebugControlsPanel;
import org.flixelgdx.debug.panels.FlixelDebugViewportPanel;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelNoopTexture;
import org.flixelgdx.group.FlixelSpriteGroup;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugViewportPanelTest {

  private static final float EPS = 1e-3f;

  private FlixelLogger originalLog;
  private FlixelDebugManager debug;
  private FlixelDebugViewportPanel viewport;
  private FlixelCamera cam;
  private FlixelState state;

  @BeforeEach
  void setUp() {
    originalLog = Flixel.log;
    FlixelDefaultLogger log = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    log.setConsoleSink(null);
    Flixel.log = log;
    debug = new FlixelDebugManager();
    viewport = debug.viewport;
    state = new TestState();
    Flixel.state = state;
    Flixel.cameras.clear();
    cam = newCamera();
    Flixel.cameras.add(cam);
  }

  @AfterEach
  void tearDown() {
    debug.controls.reset();
    Flixel.log = originalLog;
    Flixel.state = null;
    Flixel.cameras.clear();
  }

  @Test
  void theStateDescribesTheCameraAndPutsBoxesWhereObjectsAreDrawn() {
    cam.setZoom(2f);
    cam.scrollX = 50f;
    cam.scrollY = 20f;
    // At zoom 2 the camera shows 200 x 150 world units with a view margin of 100 and 75.
    FlixelObject world = new FlixelObject(200f, 120f, 20f, 10f);
    FlixelObject far = new FlixelObject(150f, 100f, 8f, 8f);
    far.setScrollFactor(0f, 0f);
    FlixelObject offScreen = new FlixelObject(5000f, 120f, 20f, 10f);
    state.add(world);
    state.add(far);
    state.add(offScreen);

    Snapshot s = Snapshot.read(viewport);

    assertEquals(0, s.camera);
    assertEquals(1, s.cameraCount);
    assertEquals(200f, s.viewWidth, EPS);
    assertEquals(150f, s.viewHeight, EPS);
    assertEquals(2f, s.zoom, EPS);
    assertEquals(50f, s.scrollX, EPS);
    assertEquals(20f, s.scrollY, EPS);
    assertFalse(s.freeCam);
    assertEquals(FlixelDebugObjectIds.NONE, s.selectedId);
    assertEquals(2, s.boxes.size(), "the object outside the view is left out");
    Box a = s.boxes.get(0);
    assertEquals(debug.ids.peek(world), a.id, "boxes come in draw order");
    assertEquals(50f, a.x, EPS);
    assertEquals(25f, a.y, EPS);
    assertEquals(20f, a.w, EPS);
    assertEquals(10f, a.h, EPS);
    assertEquals("FlixelObject", s.types.get(a.type));
    assertEquals(0, a.flags);
    Box b = s.boxes.get(1);
    assertEquals(debug.ids.peek(far), b.id);
    assertEquals(50f, b.x, EPS, "a scroll factor of 0 ignores the scroll but not the zoom");
    assertEquals(25f, b.y, EPS);
    assertEquals(viewport.getBoxCount(), s.boxes.size());
    assertEquals(50f, viewport.getBoxX(0), EPS);
  }

  @Test
  void hiddenAndRemovedObjectsAreSkippedAndFlagsAreSet() {
    FlixelObject hidden = new FlixelObject(0f, 0f, 10f, 10f);
    hidden.visible = false;
    FlixelObject gone = new FlixelObject(0f, 0f, 10f, 10f);
    gone.exists = false;
    FlixelObject asleep = new FlixelObject(0f, 0f, 10f, 10f);
    asleep.active = false;
    FlixelSpriteGroup group = new FlixelSpriteGroup();
    FlixelSprite member = new FlixelSprite(10f, 10f);
    member.setSize(40f, 40f);
    group.add(member);
    // The sprite group has no size of its own, so its box is the area around its members.
    state.add(hidden);
    state.add(gone);
    state.add(asleep);
    state.add(group);
    debug.inspector.select(member);

    Snapshot s = Snapshot.read(viewport);

    assertEquals(debug.ids.peek(member), s.selectedId);
    assertEquals(-1, s.indexOf(debug.ids.peek(hidden)));
    assertEquals(-1, s.indexOf(debug.ids.peek(gone)));
    Box sleeping = s.boxes.get(s.indexOf(debug.ids.peek(asleep)));
    assertEquals(FlixelDebugProtocol.VIEWPORT_BOX_INACTIVE, sleeping.flags);
    Box selected = s.boxes.get(s.indexOf(debug.ids.peek(member)));
    assertEquals(FlixelDebugProtocol.VIEWPORT_BOX_SELECTED, selected.flags);
    assertEquals("FlixelSprite", s.types.get(selected.type));
    int groupIndex = s.indexOf(debug.ids.peek(group));
    assertTrue(groupIndex >= 0 && groupIndex < s.indexOf(debug.ids.peek(member)), "a group is drawn below members");
    assertEquals(FlixelDebugProtocol.VIEWPORT_BOX_GROUP, s.boxes.get(groupIndex).flags);
    Box around = s.boxes.get(groupIndex);
    assertEquals(10f, around.x, EPS);
    assertEquals(10f, around.y, EPS);
    assertEquals(40f, around.w, EPS);
    assertEquals(40f, around.h, EPS);
  }

  @Test
  void anOpenSubStateIsDrawnOnTop() {
    FlixelObject lower = new FlixelObject(0f, 0f, 10f, 10f);
    state.add(lower);
    TestSubState sub = new TestSubState();
    FlixelObject upper = new FlixelObject(0f, 0f, 10f, 10f);
    sub.add(upper);
    state.openSubState(sub);

    Snapshot s = Snapshot.read(viewport);
    assertEquals(2, s.boxes.size());
    assertEquals(debug.ids.peek(upper), s.boxes.get(1).id);

    state.persistentDraw = false;
    viewport.sample();
    assertEquals(1, viewport.getBoxCount());
  }

  @Test
  void theBoxCountIsCappedAndTheTopmostObjectsAreKept() {
    int total = FlixelDebugViewportPanel.MAX_BOXES + 100;
    FlixelObject last = null;
    for (int i = 0; i < total; i++) {
      last = new FlixelObject(i % 300, 5f, 4f, 4f);
      state.add(last);
    }

    Snapshot s = Snapshot.read(viewport);

    assertEquals(FlixelDebugViewportPanel.MAX_BOXES, s.boxes.size());
    assertEquals(debug.ids.peek(last), s.boxes.get(s.boxes.size() - 1).id);
    assertEquals(debug.ids.peek(state.getMembers().get(100)), s.boxes.get(0).id, "the bottom 100 are dropped");
  }

  @Test
  void pickSelectsTheTopmostObjectAndClearsOnEmptySpace() {
    FlixelObject below = new FlixelObject(10f, 10f, 100f, 100f);
    FlixelObject above = new FlixelObject(50f, 50f, 100f, 100f);
    state.add(below);
    state.add(above);

    DebugTestSupport.send(viewport, FlixelDebugProtocol.VIEWPORT_PICK, w -> w.writeF32(60f).writeF32(60f));
    assertSame(above, debug.inspector.getSelected());
    DebugTestSupport.send(viewport, FlixelDebugProtocol.VIEWPORT_PICK, w -> w.writeF32(20f).writeF32(20f));
    assertSame(below, debug.inspector.getSelected());
    DebugTestSupport.send(viewport, FlixelDebugProtocol.VIEWPORT_PICK, w -> w.writeF32(390f).writeF32(290f));
    assertNull(debug.inspector.getSelected());
  }

  @Test
  void spriteBoxesAndPicksUseTheHitboxInsteadOfTheDrawnGraphic() {
    FlixelSprite sprite = new FlixelSprite(100f, 100f);
    sprite.setRegion(new FlixelFrame(new FlixelNoopTexture(32, 32)));
    sprite.setSize(10f, 14f);
    sprite.setOffset(3f, 2f);
    sprite.setScale(3f);
    sprite.setAngle(45f);
    state.add(sprite);

    Snapshot s = Snapshot.read(viewport);

    Box box = s.boxes.get(s.indexOf(debug.ids.peek(sprite)));
    assertEquals(100f, box.x, EPS, "offset, scale, and angle do not move the hitbox");
    assertEquals(100f, box.y, EPS);
    assertEquals(10f, box.w, EPS);
    assertEquals(14f, box.h, EPS);

    // Inside the scaled graphic but outside the hitbox.
    DebugTestSupport.send(viewport, FlixelDebugProtocol.VIEWPORT_PICK, w -> w.writeF32(130f).writeF32(130f));
    assertNull(debug.inspector.getSelected());
    DebugTestSupport.send(viewport, FlixelDebugProtocol.VIEWPORT_PICK, w -> w.writeF32(105f).writeF32(110f));
    assertSame(sprite, debug.inspector.getSelected());
  }

  @Test
  void selectedSpriteBoxCoversTheDrawnGraphicLikeTheGameOutline() {
    FlixelSprite sprite = new FlixelSprite(100f, 100f);
    sprite.setRegion(new FlixelFrame(new FlixelNoopTexture(32, 32)));
    sprite.setSize(10f, 14f);
    sprite.setOffset(3f, 2f);
    sprite.setScale(2f);
    state.add(sprite);
    debug.inspector.select(sprite);

    Snapshot s = Snapshot.read(viewport);

    FlixelDebugPicker graphic = new FlixelDebugPicker();
    assertTrue(graphic.measure(sprite, cam));
    Box box = s.boxes.get(s.indexOf(debug.ids.peek(sprite)));
    assertEquals(graphic.getBoundsLeft(), box.x, EPS);
    assertEquals(graphic.getBoundsTop(), box.y, EPS);
    assertEquals(graphic.getBoundsWidth(), box.w, EPS);
    assertEquals(graphic.getBoundsHeight(), box.h, EPS);
    assertEquals(64f, box.w, EPS, "the selected box is the scaled graphic, not the 10 by 14 hitbox");

    // Picking still tests the hitbox, so a tap inside the graphic but outside the hitbox misses.
    DebugTestSupport.send(viewport, FlixelDebugProtocol.VIEWPORT_PICK, w -> w.writeF32(140f).writeF32(140f));
    assertNull(debug.inspector.getSelected());
  }

  @Test
  void moveAddsTheDistanceToAnyObjectAndIgnoresUnknownIds() {
    FlixelObject object = new FlixelObject(10f, 20f, 5f, 5f);
    state.add(object);
    int id = debug.ids.idOf(object);

    DebugTestSupport.send(viewport, FlixelDebugProtocol.VIEWPORT_MOVE, w -> w.writeI32(id).writeF32(5f).writeF32(-3f));
    DebugTestSupport.send(viewport, FlixelDebugProtocol.VIEWPORT_MOVE, w -> w.writeI32(id).writeF32(1f).writeF32(1f));

    assertEquals(16f, object.getX(), EPS);
    assertEquals(18f, object.getY(), EPS);
    assertFalse(viewport.move(9999, 1f, 1f));
    assertFalse(viewport.move(id, Float.NaN, 1f));
    assertEquals(16f, object.getX(), EPS);
  }

  @Test
  void panTurnsTheFreeCameraOnAndMovesTheCamera() {
    FlixelObject target = new FlixelObject(1000f, 800f, 20f, 20f);
    cam.follow(target, FollowStyle.LOCKON, 1f);
    cam.scrollX = 10f;
    cam.scrollY = 20f;

    DebugTestSupport.send(viewport, FlixelDebugProtocol.VIEWPORT_PAN, w -> w.writeF32(30f).writeF32(-5f));

    assertTrue(debug.controls.isFreeCamActive());
    assertNull(cam.target, "following the target would undo the pan");
    assertEquals(40f, cam.scrollX, EPS);
    assertEquals(15f, cam.scrollY, EPS);
    viewport.sample();
    assertTrue(Snapshot.read(viewport).freeCam);
  }

  @Test
  void zoomKeepsTheAnchorStillAndClamps() {
    cam.scrollX = 100f;
    cam.scrollY = 50f;
    FlixelObject object = new FlixelObject(180f, 110f, 10f, 10f);
    state.add(object);
    viewport.sample();
    float x = viewport.getBoxX(0);
    float y = viewport.getBoxY(0);

    DebugTestSupport.send(viewport, FlixelDebugProtocol.VIEWPORT_ZOOM,
        w -> w.writeF32(2f).writeF32(x).writeF32(y));

    assertEquals(2f, cam.getZoom(), EPS);
    viewport.sample();
    // The anchor stays at the same spot of the screen, which is half as far into the half as wide view.
    assertEquals(x / 2f, viewport.getBoxX(0), EPS);
    assertEquals(y / 2f, viewport.getBoxY(0), EPS);

    assertTrue(viewport.zoom(1000f, 0f, 0f));
    assertEquals(FlixelDebugControlsPanel.MAX_ZOOM, cam.getZoom(), 0f);
    assertFalse(viewport.zoom(2f, 0f, 0f));
  }

  @Test
  void resetCameraTurnsTheFreeCameraOffAndRestoresTheCamera() {
    cam.scrollX = 12f;
    viewport.pan(100f, 0f);
    viewport.zoom(2f, 0f, 0f);

    DebugTestSupport.send(viewport, FlixelDebugProtocol.VIEWPORT_RESET_CAMERA, w -> {
    });

    assertFalse(debug.controls.isFreeCam());
    assertEquals(12f, cam.scrollX, 0f);
    assertEquals(1f, cam.getZoom(), 0f);
  }

  @Test
  void setCameraShowsAnotherCameraAndTheCameraActionsActOnIt() {
    FlixelCamera second = newCamera();
    Flixel.cameras.add(second);
    FlixelObject object = new FlixelObject(0f, 0f, 10f, 10f);
    object.cameras = new FlixelCamera[] { second };
    state.add(object);

    viewport.sample();
    assertEquals(0, viewport.getBoxCount(), "the object is not drawn on the main camera");

    DebugTestSupport.send(viewport, FlixelDebugProtocol.VIEWPORT_SET_CAMERA, w -> w.writeU8(1));
    viewport.update(0f);
    Snapshot s = Snapshot.read(viewport);
    assertEquals(1, s.camera);
    assertEquals(2, s.cameraCount);
    assertEquals(1, s.boxes.size());

    viewport.pan(25f, 0f);
    assertEquals(25f, second.scrollX, EPS);
    assertEquals(0f, cam.scrollX, 0f);

    viewport.setCameraIndex(7);
    viewport.sample();
    assertEquals(0, Snapshot.read(viewport).camera, "an index past the end shows the main camera");
  }

  @Test
  void nothingIsMarkedChangedWhenTheMapStaysTheSame() {
    FlixelObject object = new FlixelObject(10f, 10f, 10f, 10f);
    state.add(object);
    viewport.sample();
    long revision = viewport.getRevision();

    viewport.sample();
    viewport.sample();
    assertEquals(revision, viewport.getRevision());

    object.setX(11f);
    viewport.update(1f);
    assertEquals(revision, viewport.getRevision(), "timed samples are skipped while no app is connected");
    viewport.sample();
    assertTrue(viewport.getRevision() > revision);

    revision = viewport.getRevision();
    debug.controls.setHitboxes(true);
    viewport.sample();
    assertTrue(viewport.getRevision() > revision, "the controls state is part of the map");
  }

  private static FlixelCamera newCamera() {
    FlixelCamera camera = new FlixelCamera(400, 300);
    camera.getViewport().setScreenBounds(0, 0, 400, 300);
    return camera;
  }

  private record Box(int id, float x, float y, float w, float h, int type, int flags) {
  }

  /** The decoded state message. */
  private static final class Snapshot {

    final List<String> types = new ArrayList<>();
    final List<Box> boxes = new ArrayList<>();
    float viewWidth;
    float viewHeight;
    float zoom;
    float scrollX;
    float scrollY;
    int camera;
    int cameraCount;
    int selectedId;
    boolean freeCam;
    boolean paused;
    boolean hitboxes;

    static Snapshot read(FlixelDebugViewportPanel panel) {
      FlixelDebugMessageReader r = DebugTestSupport.payload(panel);
      Snapshot s = new Snapshot();
      s.camera = r.readU8();
      s.cameraCount = r.readU8();
      s.viewWidth = r.readF32();
      s.viewHeight = r.readF32();
      s.zoom = r.readF32();
      s.scrollX = r.readF32();
      s.scrollY = r.readF32();
      s.freeCam = r.readBool();
      s.paused = r.readBool();
      s.hitboxes = r.readBool();
      s.selectedId = r.readI32();
      int types = r.readCount();
      for (int i = 0; i < types; i++) {
        s.types.add(DebugTestSupport.str(r));
      }
      int boxes = r.readCount();
      for (int i = 0; i < boxes; i++) {
        s.boxes.add(new Box(r.readI32(), r.readF32(), r.readF32(), r.readF32(), r.readF32(), r.readU16(),
            r.readU8()));
      }
      assertEquals(0, r.getRemaining(), "nothing follows the boxes");
      return s;
    }

    int indexOf(int id) {
      for (int i = 0; i < boxes.size(); i++) {
        if (boxes.get(i).id == id) {
          return i;
        }
      }
      return -1;
    }
  }

  private static final class TestState extends FlixelState {
  }

  private static final class TestSubState extends FlixelSubState {
  }
}
