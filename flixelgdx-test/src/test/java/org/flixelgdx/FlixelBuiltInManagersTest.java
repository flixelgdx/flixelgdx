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

import org.flixelgdx.FlixelManagers.Manager;
import org.flixelgdx.asset.FlixelBaseAssetManager;
import org.flixelgdx.asset.FlixelNoopAssetManager;
import org.flixelgdx.audio.FlixelSoundManager;
import org.flixelgdx.backend.FlixelBootManager;
import org.flixelgdx.graphics.FlixelOverlayManager;
import org.flixelgdx.input.action.FlixelActionSets;
import org.flixelgdx.input.gamepad.FlixelGamepadInputManager;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.input.mouse.FlixelMouseInputManager;
import org.flixelgdx.input.touch.FlixelTouchManager;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.signal.FlixelSignal;
import org.flixelgdx.signal.FlixelSignalData.StateSwitchSignalData;
import org.flixelgdx.text.FlixelFontRegistry;
import org.flixelgdx.timer.FlixelTimer;
import org.flixelgdx.tween.FlixelTween;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelBuiltInManagersTest {

  private FlixelManagersTest.Recorder recorder;

  @AfterEach
  void tearDown() {
    if (recorder != null) {
      Flixel.managers.remove(recorder);
    }
  }

  @Test
  void theBuiltInOrdersAreSpacedFromInputToSound() {
    int[] orders = {
        Manager.ORDER_ASSETS, Manager.ORDER_FONTS, Manager.ORDER_KEYS, Manager.ORDER_MOUSE, Manager.ORDER_TOUCHES,
        Manager.ORDER_GAMEPADS, Manager.ORDER_ACTIONS, Manager.ORDER_DEBUG, Manager.ORDER_TWEENS, Manager.ORDER_TIMERS,
        Manager.ORDER_SOUND, Manager.ORDER_DEFAULT
    };
    for (int i = 1; i < orders.length; i++) {
      assertTrue(orders[i] - orders[i - 1] > 1, "there must be room for a manager between two built-in orders");
    }
  }

  @Test
  void everyBuiltInManagerReportsItsOwnOrder() {
    assertEquals(Manager.ORDER_KEYS, new FlixelKeyInputManager().getOrder());
    assertEquals(Manager.ORDER_MOUSE, new FlixelMouseInputManager().getOrder());
    assertEquals(Manager.ORDER_TOUCHES, new FlixelTouchManager().getOrder());
    assertEquals(Manager.ORDER_GAMEPADS, new FlixelGamepadInputManager().getOrder());
    assertEquals(Manager.ORDER_ACTIONS, FlixelActionSets.MANAGER.getOrder());
    assertEquals(Manager.ORDER_FONTS, FlixelFontRegistry.MANAGER.getOrder());
    assertEquals(Manager.ORDER_TWEENS, FlixelTween.getGlobalManager().getOrder());
    assertEquals(Manager.ORDER_TIMERS, FlixelTimer.getGlobalManager().getOrder());
    assertEquals(Manager.ORDER_SOUND, new FlixelSoundManager(FlixelSoundManager.defaultFactory).getOrder());
  }

  @Test
  void inputKeepsUpdatingWhileTheGameIsPausedButTheGameSystemsStop() {
    assertFalse(new FlixelKeyInputManager().isPausable());
    assertFalse(FlixelActionSets.MANAGER.isPausable());
    assertTrue(FlixelTween.getGlobalManager().isPausable());
    assertTrue(FlixelTimer.getGlobalManager().isPausable());
    assertTrue(new FlixelSoundManager(FlixelSoundManager.defaultFactory).isPausable());
  }

  @Test
  void theSoundSlotAlwaysReachesTheCurrentField() {
    CountingSound first = new CountingSound();
    CountingSound second = new CountingSound();
    Flixel.sound = first;
    Flixel.managers.update(0.1f, false);
    Flixel.sound = second;
    Flixel.managers.update(0.1f, false);
    assertEquals(1, first.updates);
    assertEquals(1, second.updates);
  }

  @Test
  void pausableBuiltInsSkipTheUpdateWhileThePauseIsOn() {
    CountingSound sound = new CountingSound();
    Flixel.sound = sound;
    Flixel.managers.update(0.1f, true);
    assertEquals(0, sound.updates);
  }

  @Test
  void emptyBuiltInFieldsAreSkipped() {
    Flixel.keys = null;
    Flixel.mouse = null;
    Flixel.touches = null;
    Flixel.gamepads = null;
    Flixel.sound = null;
    Flixel.debug = null;
    assertDoesNotThrow(() -> {
      Flixel.managers.preUpdate(0f, false);
      Flixel.managers.update(0f, false);
      Flixel.managers.postUpdate(0f, false);
      Flixel.managers.endFrame();
    });
  }

  @Test
  void aThirdPartyManagerSeesThisFramesInputBeforeTheDebugger() {
    StringBuilder log = new StringBuilder();
    Flixel.keys = new FlixelKeyInputManager() {
      @Override
      public void update() {
        log.append("keys ");
      }
    };
    recorder = new FlixelManagersTest.Recorder("between", Manager.ORDER_ACTIONS + 50, log);
    Flixel.managers.add(recorder);
    Flixel.managers.update(0f, false);
    assertTrue(log.toString().startsWith("keys between.update"), log.toString());
  }

  @Test
  void switchingStatesTellsTheManagersWhatTheSwitchClears() {
    Flixel.game = new FlixelGame("Flags", TestState::new) {
    };
    Flixel.boot = new FlixelBootManager();
    Flixel.boot.markInitialized();
    Flixel.sound = new FlixelSoundManager(FlixelSoundManager.defaultFactory);
    recorder = new FlixelManagersTest.Recorder("m", 0, new StringBuilder());
    Flixel.managers.add(recorder);
    Flixel.switchState(TestState::new, false, true, false);
    assertFalse(recorder.lastData.clearTweens());
    assertTrue(recorder.lastData.clearTimers());
  }

  @Test
  void closingTheGameRetiresTheAssetsAndStopsFileLoggingAfterEveryListener() {
    StringBuilder order = new StringBuilder();
    Flixel.log = new FlixelDefaultLogger() {
      @Override
      public void stopFileLogging() {
        order.append("stop ");
      }
    };
    Flixel.assets = new FlixelBaseAssetManager();
    Flixel.registerShutdownListeners();
    FlixelSignal.SignalHandler<Void> late = data -> order.append("listener ");
    Flixel.Signals.postGameClose.add(late);
    try {
      Flixel.Signals.preGameClose.dispatch();
      Flixel.Signals.postGameClose.dispatch();
    } finally {
      Flixel.Signals.postGameClose.remove(late);
    }
    assertTrue(order.toString().startsWith("listener "), order.toString());
    assertTrue(order.toString().endsWith("stop "), order.toString());
    assertSame(FlixelNoopAssetManager.INSTANCE, Flixel.assets);
  }

  @Test
  void theSceneManagersHaveSlotsInTheRegistry() {
    CountingOverlay overlay = new CountingOverlay();
    Flixel.overlay = overlay;
    overlay.setEnabled(true);
    Flixel.managers.postUpdate(0f, false);
    assertEquals(1, overlay.updates);
  }

  @Test
  void stateSwitchDataClearsEverythingByDefault() {
    StateSwitchSignalData data = new StateSwitchSignalData(null);
    assertTrue(data.clearTweens());
    assertTrue(data.clearTimers());
  }

  @Test
  void theFontManagerDisposesTheRegistryWhenDestroyed() {
    FlixelFontRegistry.MANAGER.destroy();
    assertEquals(0, FlixelFontRegistry.getRegisteredIds().getSize());
  }

  private static final class CountingSound extends FlixelSoundManager {

    int updates;

    CountingSound() {
      super(FlixelSoundManager.defaultFactory);
    }

    @Override
    public void update(float elapsed) {
      updates++;
    }
  }

  private static final class TestState extends FlixelState {
  }

  private static final class CountingOverlay extends FlixelOverlayManager {

    int updates;

    @Override
    public void postUpdate(float elapsed) {
      updates++;
    }
  }
}
