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
package org.flixelgdx;

import org.flixelgdx.audio.FlixelSoundManager;
import org.flixelgdx.backend.FlixelBootManager;
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.input.gamepad.FlixelGamepadInputManager;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.input.mouse.FlixelMouseInputManager;
import org.flixelgdx.input.touch.FlixelTouchManager;
import org.flixelgdx.testing.RecordingBatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class FlixelGameManagerHooksTest {

  private StringBuilder log;
  private FlixelGame game;
  private FlixelManagersTest.Recorder recorder;
  private FlixelManagersTest.Recorder unpausable;

  @BeforeEach
  void setUp() {
    log = new StringBuilder();
    game = new FlixelGame("Hooks", () -> new LoggingState(log)) {
    };
    Flixel.game = game;
    Flixel.state = new LoggingState(log);
    Flixel.keys = new FlixelKeyInputManager();
    Flixel.mouse = new FlixelMouseInputManager();
    Flixel.touches = new FlixelTouchManager();
    Flixel.gamepads = new FlixelGamepadInputManager();
    Flixel.sound = new FlixelSoundManager(FlixelSoundManager.defaultFactory);
    Flixel.debug = new FlixelDebugManager();
    Flixel.cameras.clear();
    recorder = new FlixelManagersTest.Recorder("m", 0, log);
    unpausable = new FlixelManagersTest.Recorder("input", 1, log);
    unpausable.pausable = false;
    Flixel.managers.add(recorder);
    Flixel.managers.add(unpausable);
  }

  @AfterEach
  void tearDown() {
    game.setGamePaused(false);
    Flixel.managers.remove(recorder);
    Flixel.managers.remove(unpausable);
  }

  @Test
  void theUpdateHooksSurroundTheStateUpdate() {
    game.update(0.016f);
    assertEquals("m.preUpdate input.preUpdate m.update input.update state.update m.postUpdate input.postUpdate ",
        log.toString());
  }

  @Test
  void aPausedGameOnlyUpdatesManagersThatAreNotPausable() {
    game.setGamePaused(true);
    log.setLength(0);
    game.update(0.016f);
    assertEquals("input.preUpdate input.update input.postUpdate ", log.toString());
  }

  @Test
  void aSteppedFrameUpdatesPausableManagersOnce() {
    game.setGamePaused(true);
    game.stepFrame();
    log.setLength(0);
    game.update(0.016f);
    game.update(0.016f);
    assertEquals("m.preUpdate input.preUpdate m.update input.update state.update m.postUpdate input.postUpdate "
        + "input.preUpdate input.update input.postUpdate ", log.toString());
  }

  @Test
  void losingFocusWithAutoPausePausesTheUpdateHooks() {
    Flixel.autoPause = true;
    game.onFocusLost();
    log.setLength(0);
    game.update(0.016f);
    assertEquals("input.preUpdate input.update input.postUpdate ", log.toString());
  }

  @Test
  void pausingTellsTheManagersOnlyWhenThePauseChanges() {
    game.setGamePaused(true);
    game.setGamePaused(true);
    game.setGamePaused(false);
    assertEquals("m.onPaused(true) input.onPaused(true) m.onPaused(false) input.onPaused(false) ", log.toString());
  }

  @Test
  void focusHooksReachTheManagersOncePerChange() {
    game.onFocusLost();
    game.onFocusLost();
    game.onFocusGained();
    game.onFocusGained();
    assertEquals("state.onFocusLost m.onFocusLost input.onFocusLost state.onFocusGained m.onFocusGained "
        + "input.onFocusGained ", log.toString());
  }

  @Test
  void suspendingReachesTheManagersOnceUntilFocusReturns() {
    game.onSuspended();
    game.onSuspended();
    assertEquals("m.onSuspended input.onSuspended ", log.toString());
  }

  @Test
  void minimizingReachesTheManagers() {
    game.onMinimized();
    assertEquals("m.onMinimized input.onMinimized ", log.toString());
  }

  @Test
  void resizingReachesTheManagersAfterTheState() {
    game.resize(800, 600);
    assertEquals("m.resize(800x600) input.resize(800x600) ", log.toString());
  }

  @Test
  void beginFrameReachesTheManagersEvenWhilePaused() {
    game.setGamePaused(true);
    log.setLength(0);
    game.beginFrame();
    assertEquals("m.beginFrame input.beginFrame ", log.toString());
  }

  @Test
  void endFrameReachesTheManagers() {
    game.endFrame();
    assertEquals("m.endFrame input.endFrame ", log.toString());
  }

  @Test
  void drawingWithoutCamerasStillRunsTheFrameHooks() {
    RecordingBatch batch = new RecordingBatch();
    game.draw(batch);
    assertEquals("m.preDraw input.preDraw m.draw input.draw m.postDraw input.postDraw ", log.toString());
    assertSame(batch, recorder.lastBatch);
  }

  @Test
  void switchingStatesRunsTheStateSwitchHooksAroundTheNewState() {
    Flixel.boot = new FlixelBootManager();
    Flixel.boot.markInitialized();
    LoggingState next = new LoggingState(log);
    Flixel.switchState(() -> next, false, false, false);
    assertEquals("m.preStateSwitch input.preStateSwitch m.onStateSwitch input.onStateSwitch state.create "
        + "m.postStateSwitch input.postStateSwitch ",
        log.toString());
    assertSame(next, recorder.lastData.state());
    assertSame(next, Flixel.state);
  }

  @Test
  void drawStateDrawsTheActiveStateAndPersistentParents() {
    LoggingState parent = new LoggingState(log);
    LoggingSubState child = new LoggingSubState(log);
    Flixel.state = parent;
    parent.persistentDraw = false;
    parent.openSubState(child);
    game.drawState(new RecordingBatch());
    assertEquals("sub.draw ", log.toString());
    log.setLength(0);
    parent.persistentDraw = true;
    game.drawState(new RecordingBatch());
    assertEquals("state.draw sub.draw ", log.toString());
  }

  private static final class LoggingState extends FlixelState {

    private final StringBuilder log;

    LoggingState(StringBuilder log) {
      this.log = log;
    }

    @Override
    public void create() {
      log.append("state.create ");
    }

    @Override
    public void update(float elapsed) {
      log.append("state.update ");
    }

    @Override
    public void draw(FlixelBatch batch) {
      log.append("state.draw ");
    }

    @Override
    public void onFocusLost() {
      log.append("state.onFocusLost ");
    }

    @Override
    public void onFocusGained() {
      log.append("state.onFocusGained ");
    }
  }

  private static final class LoggingSubState extends FlixelSubState {

    private final StringBuilder log;

    LoggingSubState(StringBuilder log) {
      super(null);
      this.log = log;
    }

    @Override
    public void draw(FlixelBatch batch) {
      log.append("sub.draw ");
    }
  }
}
