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
import org.flixelgdx.FlixelState;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.input.FlixelInputDevice;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.input.mouse.FlixelMouseButton;
import org.flixelgdx.input.mouse.FlixelMouseInputManager;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogger;
import org.flixelgdx.math.FlixelRect;
import org.flixelgdx.math.FlixelVector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugFreeCamTest {

  private static final float EPS = 1e-3f;

  private FlixelLogger originalLog;
  private FlixelInputDevice originalInput;
  private FlixelKeyInputManager originalKeys;
  private FlixelMouseInputManager originalMouse;
  private FlixelDebugManager debug;
  private FlixelDebugControlsPanel controls;
  private FlixelKeyInputManager keys;
  private FlixelMouseInputManager mouse;
  private PointerDevice device;
  private FlixelCamera cam;
  private FlixelObject target;

  @BeforeEach
  void setUp() {
    originalLog = Flixel.log;
    originalInput = Flixel.input;
    originalKeys = Flixel.keys;
    originalMouse = Flixel.mouse;
    FlixelDefaultLogger log = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    log.setConsoleSink(null);
    Flixel.log = log;
    device = new PointerDevice();
    Flixel.input = device;
    keys = new FlixelKeyInputManager();
    mouse = new FlixelMouseInputManager();
    Flixel.keys = keys;
    Flixel.mouse = mouse;
    debug = new FlixelDebugManager();
    controls = debug.controls;
    Flixel.state = new TestState();
    Flixel.cameras.clear();
    cam = newCamera();
    Flixel.cameras.add(cam);
    target = new FlixelObject(1000f, 800f, 20f, 20f);
  }

  @AfterEach
  void tearDown() {
    controls.reset();
    Flixel.log = originalLog;
    Flixel.input = originalInput;
    Flixel.keys = originalKeys;
    Flixel.mouse = originalMouse;
    Flixel.state = null;
    Flixel.cameras.clear();
  }

  @Test
  void turningItOnStopsFollowingAndTurningItOffRestoresEverythingExactly() {
    cam.follow(target, FollowStyle.PLATFORMER, 0.25f);
    cam.setScrollBoundsRect(0f, 0f, 2000f, 1500f);
    cam.setZoom(1.5f);
    cam.scrollX = 321f;
    cam.scrollY = 123f;
    FlixelRect deadzone = cam.deadzone;

    controls.setFreeCam(true);

    assertTrue(controls.isFreeCam());
    assertTrue(controls.isFreeCamActive());
    assertNull(cam.target, "the camera stops following");
    assertTrue(Float.isNaN(cam.minScrollX) && Float.isNaN(cam.maxScrollY), "the level bounds are lifted");

    cam.scrollX = -5000f;
    cam.setZoom(4f);
    controls.setFreeCam(false);

    assertFalse(controls.isFreeCamActive());
    assertSame(target, cam.target);
    assertEquals(FollowStyle.PLATFORMER, cam.style);
    assertEquals(0.25f, cam.followLerp, 0f);
    assertEquals(321f, cam.scrollX, 0f);
    assertEquals(123f, cam.scrollY, 0f);
    assertEquals(1.5f, cam.getZoom(), 0f);
    assertEquals(0f, cam.minScrollX, 0f);
    assertEquals(2000f, cam.maxScrollX, 0f);
    assertEquals(0f, cam.minScrollY, 0f);
    assertEquals(1500f, cam.maxScrollY, 0f);
    assertSame(deadzone, cam.deadzone);
  }

  @Test
  void aCameraWithoutATargetOrBoundsComesBackUnchanged() {
    cam.scrollX = 40f;
    cam.scrollY = 50f;
    controls.setFreeCam(true);
    cam.scrollX = 999f;
    controls.setFreeCam(false);

    assertEquals(40f, cam.scrollX, 0f);
    assertEquals(50f, cam.scrollY, 0f);
    assertNull(cam.target);
    assertTrue(Float.isNaN(cam.minScrollX));
  }

  @Test
  void panningMovesInScreenPixelsPerSecondSoZoomDoesNotChangeTheFeel() {
    controls.setFreeCam(true);
    float startX = cam.scrollX;
    float perPixel = 1f / cam.getZoom();

    keys.keyDown(FlixelKey.D);
    controls.updateInput(0.5f);
    assertEquals(startX + 600f * 0.5f * perPixel, cam.scrollX, EPS);

    cam.setZoom(2f);
    float before = cam.scrollX;
    controls.updateInput(0.5f);
    assertEquals(before + 600f * 0.5f / 2f, cam.scrollX, EPS, "at zoom 2 a screen pixel is half a world unit");
  }

  @Test
  void everyDirectionKeyPansAndShiftIsFourTimesFaster() {
    controls.setFreeCam(true);
    float x = cam.scrollX;
    float y = cam.scrollY;

    keys.keyDown(FlixelKey.LEFT);
    controls.updateInput(0.1f);
    assertEquals(x - 60f, cam.scrollX, EPS);
    keys.keyUp(FlixelKey.LEFT);

    keys.keyDown(FlixelKey.W);
    controls.updateInput(0.1f);
    assertEquals(y - 60f, cam.scrollY, EPS);
    keys.keyUp(FlixelKey.W);

    keys.keyDown(FlixelKey.S);
    keys.keyDown(FlixelKey.SHIFT_LEFT);
    float beforeY = cam.scrollY;
    controls.updateInput(0.1f);
    assertEquals(beforeY + 240f, cam.scrollY, EPS);
  }

  @Test
  void aDiagonalIsNotFasterThanAStraightLine() {
    controls.setFreeCam(true);
    float x = cam.scrollX;
    float y = cam.scrollY;
    keys.keyDown(FlixelKey.D);
    keys.keyDown(FlixelKey.S);
    controls.updateInput(1f);
    double distance = Math.hypot(cam.scrollX - x, cam.scrollY - y);
    assertEquals(600.0, distance, 0.5);
  }

  @Test
  void theSpeedIsConfigurable() {
    controls.setFreeCamSpeed(100f);
    assertEquals(100f, controls.getFreeCamSpeed(), 0f);
    controls.setFreeCam(true);
    float x = cam.scrollX;
    keys.keyDown(FlixelKey.D);
    controls.updateInput(1f);
    assertEquals(x + 100f, cam.scrollX, EPS);
    controls.setFreeCamSpeed(-5f);
    assertEquals(1f, controls.getFreeCamSpeed(), 0f);
  }

  @Test
  void theWheelAndQAndEZoomInSteps() {
    controls.setFreeCam(true);
    mouse.scrolled(0f, -1f);
    controls.updateInput(0.016f);
    assertEquals(1.25f, cam.getZoom(), EPS);

    mouse.endFrame();
    mouse.scrolled(0f, 2f);
    controls.updateInput(0.016f);
    assertEquals(1.25f / (1.25f * 1.25f), cam.getZoom(), EPS);

    cam.setZoom(1f);
    mouse.endFrame();
    keys.keyDown(FlixelKey.E);
    controls.updateInput(1f);
    assertEquals((float) Math.pow(1.25, 3), cam.getZoom(), EPS);
    keys.keyUp(FlixelKey.E);
    keys.keyDown(FlixelKey.Q);
    controls.updateInput(1f);
    assertEquals(1f, cam.getZoom(), 1e-2f);
  }

  @Test
  void theZoomIsClampedBetweenATenthAndTen() {
    controls.setFreeCam(true);
    for (int i = 0; i < 40; i++) {
      mouse.endFrame();
      mouse.scrolled(0f, -1f);
      controls.updateInput(0.016f);
    }
    assertEquals(10f, cam.getZoom(), 0f);

    for (int i = 0; i < 80; i++) {
      mouse.endFrame();
      mouse.scrolled(0f, 1f);
      controls.updateInput(0.016f);
    }
    assertEquals(0.1f, cam.getZoom(), 0f);
  }

  @Test
  void dragWithTheRightButtonPansTheOppositeWayToTheMouse() {
    controls.setFreeCam(true);
    float x = cam.scrollX;
    float y = cam.scrollY;
    device.buttons[FlixelMouseButton.RIGHT] = true;
    device.x = 100;
    device.y = 100;
    mouse.update();
    controls.updateInput(0.016f);
    assertEquals(x, cam.scrollX, 0f, "the first frame only grabs");

    mouse.endFrame();
    device.x = 130;
    device.y = 90;
    mouse.update();
    controls.updateInput(0.016f);
    assertEquals(x - 30f, cam.scrollX, EPS);
    assertEquals(y + 10f, cam.scrollY, EPS);

    mouse.endFrame();
    device.buttons[FlixelMouseButton.RIGHT] = false;
    device.x = 500;
    mouse.update();
    controls.updateInput(0.016f);
    assertEquals(x - 30f, cam.scrollX, EPS, "letting go stops the drag");
  }

  @Test
  void theGameDoesNotSeeTheFreeCameraInputWhileItIsOnAndSeesItAgainAfterwards() {
    controls.setFreeCam(true);
    keys.keyDown(FlixelKey.W);
    keys.keyDown(FlixelKey.SPACE);
    device.buttons[FlixelMouseButton.RIGHT] = true;
    mouse.scrolled(0f, -1f);
    mouse.update();

    assertFalse(keys.pressed(FlixelKey.W));
    assertTrue(keys.pressed(FlixelKey.SPACE), "other keys still reach the game");
    assertFalse(mouse.pressed(FlixelMouseButton.RIGHT));
    assertEquals(0f, mouse.getScrollDeltaY(), 0f);

    controls.setFreeCam(false);

    assertTrue(keys.pressed(FlixelKey.W));
    assertTrue(mouse.pressed(FlixelMouseButton.RIGHT));
    assertEquals(-1f, mouse.getScrollDeltaY(), 0f);
  }

  @Test
  void goToTurnsTheFreeCameraOnAndCentersOnThePoint() {
    cam.follow(target);
    assertFalse(controls.isFreeCam());

    assertTrue(controls.goTo(640f, 360f));

    assertTrue(controls.isFreeCamActive());
    assertNull(cam.target, "the follow target no longer pulls the camera away");
    assertEquals(640f, centerX(cam), EPS);
    assertEquals(360f, centerY(cam), EPS);

    cam.update(0.016f);
    assertEquals(640f, centerX(cam), EPS, "a camera update leaves the move alone");

    assertTrue(controls.goTo(-20f, 5f));
    assertEquals(-20f, centerX(cam), EPS);
    assertEquals(5f, centerY(cam), EPS);
  }

  @Test
  void goToAtAnyZoomCentersOnThePoint() {
    cam.setZoom(2.5f);
    assertTrue(controls.goTo(500f, 400f));
    assertEquals(500f, centerX(cam), EPS);
    assertEquals(400f, centerY(cam), EPS);
  }

  @Test
  void goToWithoutACameraDoesNothing() {
    Flixel.cameras.clear();
    assertFalse(controls.goTo(1f, 2f));
    assertFalse(controls.isFreeCamActive());
  }

  @Test
  void theFreeCameraStartsLaterWhenItWasRequestedBeforeACameraExisted() {
    Flixel.cameras.clear();
    controls.setFreeCam(true);
    assertTrue(controls.isFreeCam());
    assertFalse(controls.isFreeCamActive());

    Flixel.cameras.add(cam);
    controls.updateInput(0.016f);

    assertTrue(controls.isFreeCamActive());
  }

  @Test
  void aReplacedMainCameraIsFlownInsteadAndTheOldOneIsRestored() {
    cam.scrollX = 77f;
    controls.setFreeCam(true);
    cam.scrollX = 5000f;

    FlixelCamera replacement = newCamera();
    Flixel.cameras.clear();
    Flixel.cameras.add(replacement);
    controls.updateInput(0.016f);

    assertEquals(77f, cam.scrollX, 0f);
    assertTrue(controls.isFreeCamActive());
    keys.keyDown(FlixelKey.D);
    controls.updateInput(1f);
    assertEquals(600f / replacement.getZoom(), replacement.scrollX, EPS);
  }

  @Test
  void aTargetGivenToTheCameraWhileFlyingIsIgnored() {
    controls.setFreeCam(true);
    cam.follow(target);
    controls.updateInput(0.016f);
    assertNull(cam.target);
  }

  @Test
  void theHotkeyTogglesTheFreeCameraAndCanBeRebound() {
    tap(FlixelKey.F4);
    assertTrue(controls.isFreeCamActive());
    tap(FlixelKey.F4);
    assertFalse(controls.isFreeCam());
    assertFalse(controls.isFreeCamActive());

    controls.setFreeCamKey(FlixelKey.F6);
    tap(FlixelKey.F4);
    assertFalse(controls.isFreeCam(), "F4 does nothing once it is rebound");
    tap(FlixelKey.F6);
    assertTrue(controls.isFreeCam());
  }

  @Test
  void resetTurnsTheFreeCameraOffAndRestoresTheCamera() {
    cam.scrollX = 12f;
    controls.setFreeCam(true);
    cam.scrollX = 99f;
    controls.reset();
    assertFalse(controls.isFreeCam());
    assertEquals(12f, cam.scrollX, 0f);
    assertFalse(keys.isBlocked(FlixelKey.W));
  }

  @Test
  void consoleCommandsDriveTheFreeCamera() {
    assertTrue(debug.console.execute("freecam on"));
    assertTrue(controls.isFreeCamActive());
    assertTrue(debug.console.execute("freecam"));
    assertFalse(controls.isFreeCam(), "no argument toggles");
    assertTrue(debug.console.execute("freecam off"));
    assertFalse(controls.isFreeCam());

    assertTrue(debug.console.execute("goto 640 360"));
    assertTrue(controls.isFreeCamActive());
    assertEquals(640f, centerX(cam), EPS);
    assertEquals(360f, centerY(cam), EPS);
    assertTrue(debug.console.execute("goto 10.5 -20"));
    assertEquals(10.5f, centerX(cam), EPS);
    assertEquals(-20f, centerY(cam), EPS);

    float x = cam.scrollX;
    assertTrue(debug.console.execute("goto 1"), "a bad call logs a warning and changes nothing");
    assertTrue(debug.console.execute("goto a b"));
    assertEquals(x, cam.scrollX, 0f);
  }

  @Test
  void theNewCommandsHaveUsageAndDescription() {
    assertTrue(debug.console.has("freecam"));
    assertTrue(debug.console.has("goto"));
    assertTrue(debug.console.has("select"));
    assertCommand("freecam", "freecam [on|off]");
    assertCommand("goto", "goto <x> <y>");
    assertCommand("select", "select [path]");
  }

  @Test
  void anAltClickSelectsTheObjectUnderTheMouseAndTheGameNeverSeesTheClick() {
    FlixelObject object = new FlixelObject(100f, 100f, 50f, 50f);
    Flixel.state.add(object);
    placeMouseOverWorld(120f, 120f);

    keys.keyDown(FlixelKey.ALT_LEFT);
    device.buttons[FlixelMouseButton.LEFT] = true;
    mouse.update();
    assertTrue(mouse.justPressed(FlixelMouseButton.LEFT), "before the debugger runs, the game could see it");

    Object[] seen = new Object[1];
    debug.inspector.addSelectionListener(selected -> seen[0] = selected);
    debug.updateInput(0.016f);

    assertSame(object, debug.inspector.getSelected());
    assertSame(object, seen[0], "the selection listener is told");
    assertFalse(mouse.justPressed(FlixelMouseButton.LEFT));
    assertFalse(mouse.pressed(FlixelMouseButton.LEFT));
    assertTrue(mouse.rawPressed(FlixelMouseButton.LEFT));
  }

  @Test
  void aPlainClickIsNotAPickAndAnAltClickOnNothingKeepsTheSelection() {
    FlixelObject object = new FlixelObject(100f, 100f, 50f, 50f);
    Flixel.state.add(object);
    placeMouseOverWorld(120f, 120f);
    device.buttons[FlixelMouseButton.LEFT] = true;
    mouse.update();

    debug.updateInput(0.016f);

    assertNull(debug.inspector.getSelected());
    assertTrue(mouse.justPressed(FlixelMouseButton.LEFT), "a normal click reaches the game");

    debug.inspector.select(object);
    mouse.endFrame();
    device.buttons[FlixelMouseButton.LEFT] = false;
    mouse.update();
    mouse.endFrame();
    placeMouseOverWorld(350f, 250f);
    keys.keyDown(FlixelKey.ALT_RIGHT);
    device.buttons[FlixelMouseButton.LEFT] = true;
    mouse.update();
    debug.updateInput(0.016f);
    assertSame(object, debug.inspector.getSelected());
    assertFalse(mouse.justPressed(FlixelMouseButton.LEFT), "it still counted as a debugger click");
  }

  @Test
  void theSelectCommandWithoutAPathPicksUnderTheMouse() {
    FlixelObject object = new FlixelObject(100f, 100f, 50f, 50f);
    Flixel.state.add(object);
    placeMouseOverWorld(120f, 120f);
    mouse.update();

    assertTrue(debug.console.execute("select"));

    assertSame(object, debug.inspector.getSelected());
    debug.inspector.clearSelection();
    assertTrue(debug.console.execute("select state.0"));
    assertSame(object, debug.inspector.getSelected());
  }

  @Test
  void anObjectCanHaveACustomDebugColor() {
    FlixelObject object = new FlixelObject();
    assertNull(object.getDebugBoundingBoxColorOverride());
    float[] pink = { 1f, 0f, 1f, 1f };
    object.setDebugBoundingBoxColor(pink);
    assertSame(pink, object.getDebugBoundingBoxColorOverride());
    assertSame(pink, object.getDebugBoundingBoxColor());
    object.setDebugBoundingBoxColor(null);
    assertNull(object.getDebugBoundingBoxColorOverride());
  }

  /** Presses and releases a key over two frames, running the controls' per-frame update on the press frame. */
  private void tap(int key) {
    keys.keyDown(key);
    controls.update(0.016f);
    keys.endFrame();
    keys.keyUp(key);
    keys.endFrame();
  }

  /** A camera whose viewport maps one window pixel to one world unit at zoom 1, like a real window of that size. */
  private static FlixelCamera newCamera() {
    FlixelCamera camera = new FlixelCamera(400, 300);
    camera.getViewport().setScreenBounds(0, 0, 400, 300);
    return camera;
  }

  private void assertCommand(String name, String usage) {
    FlixelDebugMessageReader r = DebugTestSupport.payload(debug.console);
    assertEquals(FlixelDebugProtocol.CONSOLE_COMMANDS, r.readU8());
    int count = r.readCount();
    for (int i = 0; i < count; i++) {
      String commandName = DebugTestSupport.str(r);
      String commandUsage = DebugTestSupport.str(r);
      String description = DebugTestSupport.str(r);
      if (commandName.equals(name)) {
        assertEquals(usage, commandUsage);
        assertFalse(description.isEmpty(), name + " has a description");
        return;
      }
    }
    throw new AssertionError("Command not listed: " + name);
  }

  /** Moves the fake pointer to the screen position that shows the given view-space point. */
  private void placeMouseOverWorld(float viewX, float viewY) {
    cam.getViewport().setScreenBounds(0, 0, 400, 300);
    cam.getViewport().setCameraPosition(200f, 150f);
    FlixelVector screen = new FlixelVector(viewX, viewY);
    cam.getViewport().project(screen);
    device.x = Math.round(screen.x);
    device.y = Math.round(screen.y);
  }

  private static float centerX(FlixelCamera camera) {
    return camera.scrollX + camera.getViewMarginX() + camera.getWorldWidth() / (2f * camera.getZoom());
  }

  private static float centerY(FlixelCamera camera) {
    return camera.scrollY + camera.getViewMarginY() + camera.getWorldHeight() / (2f * camera.getZoom());
  }

  private static final class TestState extends FlixelState {
  }

  private static final class PointerDevice implements FlixelInputDevice {

    final boolean[] buttons = new boolean[8];
    int x;
    int y;

    @Override
    public boolean isButtonPressed(int button) {
      return button >= 0 && button < buttons.length && buttons[button];
    }

    @Override
    public int getX() {
      return x;
    }

    @Override
    public int getY() {
      return y;
    }
  }
}
