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
import org.flixelgdx.FlixelGame;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.FlixelState;
import org.flixelgdx.audio.FlixelSoundManager;
import org.flixelgdx.debug.panels.FlixelDebugInputPanel;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.input.FlixelBaseInputDevice;
import org.flixelgdx.input.FlixelInputDevice;
import org.flixelgdx.input.FlixelKeyboardListener;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.input.mouse.FlixelMouseButton;
import org.flixelgdx.input.mouse.FlixelMouseInputManager;
import org.flixelgdx.input.touch.FlixelTouchManager;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugInputPanelTest {

  private FlixelLogger originalLog;
  private FlixelInputDevice originalInput;
  private FlixelKeyInputManager originalKeys;
  private FlixelMouseInputManager originalMouse;
  private FlixelTouchManager originalTouches;
  private FlixelGame originalGame;
  private FlixelSoundManager originalSound;
  private FlixelDebugManager originalDebug;
  private FlixelDebugManager debug;
  private FlixelDebugInputPanel input;
  private FlixelKeyInputManager keys;
  private FlixelMouseInputManager mouse;
  private FlixelTouchManager touches;
  private FlixelGame game;
  private FlixelCamera cam;
  private TestDevice device;
  private final StringBuilder typed = new StringBuilder();

  @BeforeEach
  void setUp() {
    originalLog = Flixel.log;
    originalInput = Flixel.input;
    originalKeys = Flixel.keys;
    originalMouse = Flixel.mouse;
    originalTouches = Flixel.touches;
    originalGame = Flixel.game;
    originalSound = Flixel.sound;
    originalDebug = Flixel.debug;
    FlixelDefaultLogger log = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    log.setConsoleSink(null);
    Flixel.log = log;
    device = new TestDevice();
    Flixel.input = device;
    keys = new FlixelKeyInputManager();
    mouse = new FlixelMouseInputManager();
    touches = new FlixelTouchManager();
    Flixel.keys = keys;
    Flixel.mouse = mouse;
    Flixel.touches = touches;
    device.addKeyboardListener(keys);
    device.addMouseListener(mouse);
    device.addTouchListener(touches);
    device.addKeyboardListener(new FlixelKeyboardListener() {
      @Override
      public void keyTyped(char character) {
        typed.append(character);
      }
    });
    FlixelState state = new TestState();
    game = new FlixelGame("Input test", () -> state) {
    };
    Flixel.game = game;
    Flixel.sound = new FlixelSoundManager(FlixelSoundManager.defaultFactory);
    debug = new FlixelDebugManager();
    Flixel.debug = debug;
    input = debug.input;
    Flixel.state = state;
    Flixel.cameras.clear();
    cam = new FlixelCamera(400, 300);
    cam.getViewport().setScreenBounds(0, 0, 400, 300);
    Flixel.cameras.add(cam);
  }

  @AfterEach
  void tearDown() {
    game.setGamePaused(false);
    debug.controls.reset();
    input.reset();
    Flixel.log = originalLog;
    Flixel.input = originalInput;
    Flixel.keys = originalKeys;
    Flixel.mouse = originalMouse;
    Flixel.touches = originalTouches;
    Flixel.game = originalGame;
    Flixel.sound = originalSound;
    Flixel.debug = originalDebug;
    Flixel.state = null;
    Flixel.cameras.clear();
  }

  /** Runs the input half of one game frame the way FlixelGame does, then ends the frame. */
  private void frame() {
    input.applyQueued();
    keys.update();
    mouse.update();
    touches.update();
  }

  private void endFrame() {
    keys.endFrame();
    mouse.endFrame();
    touches.endFrame();
  }

  @Test
  void aKeyTapIsSeenByEveryReadPathAndNeedsTwoFrames() {
    assertTrue(input.key(FlixelKey.SPACE, true));
    assertTrue(input.key(FlixelKey.SPACE, false));

    frame();
    assertTrue(keys.pressed(FlixelKey.SPACE));
    assertTrue(keys.rawPressed(FlixelKey.SPACE));
    assertTrue(keys.justPressed(FlixelKey.SPACE));
    assertTrue(keys.anyPressed(FlixelKey.SPACE, FlixelKey.A));
    assertEquals(FlixelKey.SPACE, keys.firstPressed());
    assertFalse(keys.justReleased(FlixelKey.SPACE));
    endFrame();

    frame();
    assertFalse(keys.pressed(FlixelKey.SPACE));
    assertTrue(keys.justReleased(FlixelKey.SPACE));
    assertFalse(keys.justPressed(FlixelKey.SPACE));
    endFrame();
    assertEquals(0, input.getQueued());
  }

  @Test
  void aHeldKeyStaysDownAndInvalidKeysAreRefused() {
    assertFalse(input.key(-5, true));
    assertFalse(input.key(FlixelKey.MAX_KEYCODE + 1, true));
    input.key(FlixelKey.D, true);
    for (int i = 0; i < 3; i++) {
      frame();
      assertTrue(keys.pressed(FlixelKey.D));
      endFrame();
    }
    assertTrue(keys.isInjected(FlixelKey.D));
  }

  @Test
  void theFreeCameraNeitherHidesInjectedKeysNorMovesFromThem() {
    debug.controls.setFreeCam(true);
    debug.controls.updateInput(0.1f);
    assertFalse(keys.isInjected(FlixelKey.W));
    float scrollY = cam.scrollY;

    input.key(FlixelKey.W, true);
    frame();
    assertTrue(keys.pressed(FlixelKey.W), "an injected key is not hidden by the free camera");
    assertTrue(keys.justPressed(FlixelKey.W));
    debug.controls.updateInput(1f);
    assertEquals(scrollY, cam.scrollY, 1e-4f, "an injected W does not fly the free camera");

    device.keyDownReal(FlixelKey.S);
    assertFalse(keys.pressed(FlixelKey.S), "a real key the free camera uses is still hidden");
    device.keyUpReal(FlixelKey.S);
  }

  @Test
  void textIsTypedLikeKeyboardCharactersAndPressesNoKey() {
    assertFalse(input.type(""));
    assertTrue(input.type("Hi!"));
    frame();
    assertEquals("Hi!", typed.toString());
    assertFalse(keys.anyPressed(FlixelKey.H, FlixelKey.I));
  }

  @Test
  void aPointerLandsOnTheViewPointAndAClickNeedsTwoFrames() {
    cam.scrollX = 40f;
    cam.scrollY = 10f;
    input.pointer(FlixelDebugProtocol.INPUT_POINTER_DOWN, FlixelMouseButton.LEFT, 100f, 60f);
    input.pointer(FlixelDebugProtocol.INPUT_POINTER_UP, FlixelMouseButton.LEFT, 100f, 60f);

    frame();
    assertTrue(mouse.justPressed(FlixelMouseButton.LEFT));
    assertTrue(mouse.pressed(FlixelMouseButton.LEFT));
    assertEquals(mouse.getWorldX(cam), mouse.getWorldX(), 1e-3f);
    assertEquals(100f + cam.getViewX(), mouse.getWorldX(cam), 1f, "world x of the view point");
    assertEquals(60f + cam.getViewY(), mouse.getWorldY(cam), 1f, "world y of the view point");
    endFrame();

    frame();
    assertTrue(mouse.justReleased(FlixelMouseButton.LEFT));
    assertFalse(mouse.pressed(FlixelMouseButton.LEFT));
    endFrame();
  }

  @Test
  void anInjectedRightClickAndScrollAreNotHiddenByTheFreeCamera() {
    debug.controls.setFreeCam(true);
    debug.controls.updateInput(0.1f);
    assertTrue(mouse.isBlocked(FlixelMouseButton.RIGHT));

    input.scroll(3f);
    input.pointer(FlixelDebugProtocol.INPUT_POINTER_DOWN, FlixelMouseButton.RIGHT, 10f, 10f);
    frame();
    assertTrue(mouse.pressed(FlixelMouseButton.RIGHT));
    assertTrue(mouse.justPressed(FlixelMouseButton.RIGHT));
    assertEquals(3f, mouse.getScrollDeltaY(), 1e-4f);
    assertEquals(0f, mouse.getRawScrollDeltaY(), 1e-4f, "the free camera does not zoom from injected scroll");
    endFrame();
    assertEquals(0f, mouse.getScrollDeltaY(), 1e-4f);
  }

  @Test
  void clickingAnObjectPressesAtTheCenterOfItsBox() {
    FlixelObject target = new FlixelObject(100f, 50f, 40f, 20f);
    Flixel.state.add(target);
    int id = debug.ids.idOf(target);

    assertTrue(input.click(id, FlixelMouseButton.LEFT));
    frame();
    assertTrue(mouse.justPressed(FlixelMouseButton.LEFT));
    assertTrue(mouse.overlap(target));
    assertEquals(120f, mouse.getWorldX(cam), 1f);
    assertEquals(60f, mouse.getWorldY(cam), 1f);
    endFrame();

    frame();
    assertTrue(mouse.justReleased(FlixelMouseButton.LEFT));
    endFrame();
    assertEquals(0, input.getQueued());
  }

  @Test
  void clickingAnObjectOnAnotherCameraUsesThatCamera() {
    FlixelCamera hud = new FlixelCamera(400, 300);
    hud.getViewport().setScreenBounds(0, 0, 400, 300);
    hud.defaultDrawTarget = false;
    cam.defaultDrawTarget = false;
    Flixel.cameras.add(hud);
    FlixelObject button = new FlixelObject(30f, 40f, 20f, 10f);
    button.cameras = new FlixelCamera[] { hud };
    Flixel.state.add(button);
    assertEquals(0, debug.viewport.getCameraIndex(), "the viewport shows the other camera");

    input.click(debug.ids.idOf(button), FlixelMouseButton.LEFT);
    frame();
    assertTrue(mouse.justPressed(FlixelMouseButton.LEFT));
    assertEquals(40f, mouse.getWorldX(hud), 1f);
    assertEquals(45f, mouse.getWorldY(hud), 1f);
    endFrame();
    frame();
    assertTrue(mouse.justReleased(FlixelMouseButton.LEFT));
    endFrame();

    button.cameras = new FlixelCamera[0];
    input.click(debug.ids.idOf(button), FlixelMouseButton.LEFT);
    frame();
    assertFalse(mouse.pressed(FlixelMouseButton.LEFT), "no camera draws it now");
  }

  @Test
  void clickingAnUnknownObjectDoesNothing() {
    input.click(9999, FlixelMouseButton.LEFT);
    frame();
    assertFalse(mouse.pressed(FlixelMouseButton.LEFT));
    assertEquals(0, input.getQueued());
  }

  @Test
  void pointerTouchesStartOnlyWhenTouchInputIsEnabled() {
    input.pointer(FlixelDebugProtocol.INPUT_POINTER_DOWN, FlixelMouseButton.LEFT, 20f, 30f);
    frame();
    assertFalse(touches.pressed(0));
    endFrame();
    input.pointer(FlixelDebugProtocol.INPUT_POINTER_UP, FlixelMouseButton.LEFT, 20f, 30f);
    frame();
    endFrame();

    touches.enabled = true;
    input.pointer(FlixelDebugProtocol.INPUT_POINTER_DOWN, FlixelMouseButton.LEFT, 20f, 30f);
    frame();
    assertTrue(touches.pressed(0));
    assertTrue(touches.justPressed(0));
    endFrame();
    input.pointer(FlixelDebugProtocol.INPUT_POINTER_UP, FlixelMouseButton.LEFT, 20f, 30f);
    frame();
    assertTrue(touches.justReleased(0));
    assertFalse(touches.pressed(0));
    endFrame();
  }

  @Test
  void aPausedGameKeepsTheQueueUntilItStepsOrResumes() {
    game.setGamePaused(true);
    input.key(FlixelKey.SPACE, true);
    frame();
    assertFalse(keys.pressed(FlixelKey.SPACE), "a paused game does not see the input yet");
    assertEquals(1, input.getQueued());
    endFrame();

    game.stepFrame();
    frame();
    assertTrue(keys.justPressed(FlixelKey.SPACE), "the step frame sees it as just pressed");
    assertEquals(0, input.getQueued());
    endFrame();

    input.key(FlixelKey.SPACE, false);
    game.setGamePaused(false);
    frame();
    assertTrue(keys.justReleased(FlixelKey.SPACE));
  }

  @Test
  void resetReleasesEverythingThatIsHeldAndEmptiesTheQueue() {
    input.key(FlixelKey.A, true);
    input.pointer(FlixelDebugProtocol.INPUT_POINTER_DOWN, FlixelMouseButton.LEFT, 5f, 5f);
    frame();
    endFrame();
    frame();
    assertTrue(keys.pressed(FlixelKey.A));
    assertTrue(mouse.pressed(FlixelMouseButton.LEFT));
    input.key(FlixelKey.B, true);
    input.type("x");

    input.reset();

    assertFalse(keys.pressed(FlixelKey.A));
    assertFalse(mouse.pressed(FlixelMouseButton.LEFT));
    assertEquals(0, input.getQueued());
  }

  @Test
  void aFullQueueDropsTheRest() {
    for (int i = 0; i < FlixelDebugInputPanel.CAPACITY; i++) {
      assertTrue(input.scroll(1f));
    }
    assertFalse(input.scroll(1f));
    assertEquals(1, input.getDropped());
  }

  @Test
  void theStateReportsTheSupportedFlags() {
    var r = DebugTestSupport.payload(input);
    int flags = r.readU8();
    assertEquals(0, r.getRemaining());
    assertEquals(FlixelDebugProtocol.INPUT_FLAG_KEYS | FlixelDebugProtocol.INPUT_FLAG_POINTER
        | FlixelDebugProtocol.INPUT_FLAG_SCROLL | FlixelDebugProtocol.INPUT_FLAG_TEXT, flags);
    touches.enabled = true;
    assertEquals(FlixelDebugProtocol.INPUT_FLAG_TOUCH,
        DebugTestSupport.payload(input).readU8() & FlixelDebugProtocol.INPUT_FLAG_TOUCH);
  }

  @Test
  void actionsFromTheAppAreDecoded() {
    DebugTestSupport.send(input, FlixelDebugProtocol.INPUT_KEY, w -> w.writeBool(true).writeI32(FlixelKey.A));
    DebugTestSupport.send(input, FlixelDebugProtocol.INPUT_TEXT, w -> w.writeString("ok"));
    DebugTestSupport.send(input, FlixelDebugProtocol.INPUT_SCROLL, w -> w.writeF32(2f));
    DebugTestSupport.send(input, FlixelDebugProtocol.INPUT_POINTER,
        w -> w.writeU8(FlixelDebugProtocol.INPUT_POINTER_MOVE).writeU8(0).writeF32(1f).writeF32(2f));
    DebugTestSupport.send(input, FlixelDebugProtocol.INPUT_CLICK_OBJECT, w -> w.writeI32(77).writeU8(0));
    DebugTestSupport.send(input, 99, w -> w.writeU8(1));
    assertEquals(5, input.getQueued());
    frame();
    assertTrue(keys.pressed(FlixelKey.A));
    endFrame();
    frame();
    assertEquals("ok", typed.toString());
  }

  private static final class TestDevice extends FlixelBaseInputDevice {

    void keyDownReal(int key) {
      dispatchKeyDown(key);
    }

    void keyUpReal(int key) {
      dispatchKeyUp(key);
    }
  }

  private static final class TestState extends FlixelState {
  }
}
