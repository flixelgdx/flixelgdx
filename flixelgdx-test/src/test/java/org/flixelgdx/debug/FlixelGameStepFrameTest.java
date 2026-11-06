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
import org.flixelgdx.FlixelGame;
import org.flixelgdx.FlixelState;
import org.flixelgdx.audio.FlixelSoundManager;
import org.flixelgdx.input.gamepad.FlixelGamepadInputManager;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.input.mouse.FlixelMouseInputManager;
import org.flixelgdx.input.touch.FlixelTouchManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelGameStepFrameTest {

  private FlixelGame game;
  private CountingState state;
  private FlixelDebugManager debug;
  private FlixelGame originalGame;
  private FlixelState originalState;
  private FlixelKeyInputManager originalKeys;
  private FlixelMouseInputManager originalMouse;
  private FlixelTouchManager originalTouches;
  private FlixelGamepadInputManager originalGamepads;
  private FlixelSoundManager originalSound;
  private FlixelDebugManager originalDebug;

  @BeforeEach
  void setUp() {
    originalGame = Flixel.game;
    originalState = Flixel.state;
    originalKeys = Flixel.keys;
    originalMouse = Flixel.mouse;
    originalTouches = Flixel.touches;
    originalGamepads = Flixel.gamepads;
    originalSound = Flixel.sound;
    originalDebug = Flixel.debug;
    state = new CountingState();
    game = new FlixelGame("Step test", () -> state) {
    };
    Flixel.game = game;
    Flixel.state = state;
    Flixel.keys = new FlixelKeyInputManager();
    Flixel.mouse = new FlixelMouseInputManager();
    Flixel.touches = new FlixelTouchManager();
    Flixel.gamepads = new FlixelGamepadInputManager();
    Flixel.sound = new FlixelSoundManager(FlixelSoundManager.defaultFactory);
    debug = new FlixelDebugManager();
    Flixel.debug = debug;
  }

  @AfterEach
  void tearDown() {
    game.setGamePaused(false);
    Flixel.game = originalGame;
    Flixel.state = originalState;
    Flixel.keys = originalKeys;
    Flixel.mouse = originalMouse;
    Flixel.touches = originalTouches;
    Flixel.gamepads = originalGamepads;
    Flixel.sound = originalSound;
    Flixel.debug = originalDebug;
  }

  @Test
  void aRunningGameUpdatesEveryFrame() {
    game.update(0.016f);
    game.update(0.016f);
    assertEquals(2, state.updates);
  }

  @Test
  void aPausedGameDoesNotUpdate() {
    game.setGamePaused(true);
    game.update(0.016f);
    game.update(0.016f);
    assertEquals(0, state.updates);
  }

  @Test
  void stepFrameRunsExactlyOneUpdateWhilePaused() {
    game.setGamePaused(true);
    game.stepFrame();
    game.update(0.016f);
    assertEquals(1, state.updates);
    game.update(0.016f);
    game.update(0.016f);
    assertEquals(1, state.updates, "the game stays paused after the step");
    assertTrue(game.isGamePaused());
  }

  @Test
  void callingStepFrameTwiceBeforeTheUpdateStillStepsOnce() {
    game.setGamePaused(true);
    game.stepFrame();
    game.stepFrame();
    game.update(0.016f);
    game.update(0.016f);
    assertEquals(1, state.updates);
  }

  @Test
  void stepFrameWhileRunningLeavesNoPendingStep() {
    game.stepFrame();
    game.update(0.016f);
    assertEquals(1, state.updates);
    game.setGamePaused(true);
    game.update(0.016f);
    assertEquals(1, state.updates, "a step requested while running must not carry over to a later pause");
  }

  @Test
  void theControlsPanelSteps() {
    assertFalse(debug.controls.stepFrame());
    debug.controls.setPaused(true);
    assertTrue(debug.controls.isPaused());
    assertTrue(debug.controls.stepFrame());
    game.update(0.016f);
    game.update(0.016f);
    assertEquals(1, state.updates);
  }

  private static final class CountingState extends FlixelState {

    int updates;

    @Override
    public void update(float elapsed) {
      updates++;
    }
  }
}
