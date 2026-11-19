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
import org.flixelgdx.collections.FlixelBooleanArray;
import org.flixelgdx.signal.FlixelSignal.SignalHandler;
import org.flixelgdx.signal.FlixelSignalData.GamePausedSignalData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelGamePausedSignalTest {

  private FlixelGame game;
  private FlixelGame originalGame;
  private FlixelSoundManager originalSound;
  private FlixelBooleanArray received;
  private SignalHandler<GamePausedSignalData> listener;

  @BeforeEach
  void setUp() {
    originalGame = Flixel.game;
    originalSound = Flixel.sound;
    game = new FlixelGame("Pause signal test", () -> new FlixelState() {
    }) {
    };
    Flixel.game = game;
    Flixel.sound = new FlixelSoundManager(FlixelSoundManager.defaultFactory);
    received = new FlixelBooleanArray();
    listener = data -> received.add(data.paused());
    Flixel.Signals.gamePaused.add(listener);
  }

  @AfterEach
  void tearDown() {
    Flixel.Signals.gamePaused.remove(listener);
    game.setGamePaused(false);
    Flixel.game = originalGame;
    Flixel.sound = originalSound;
  }

  @Test
  void pausingAndResumingDispatchesTheNewValue() {
    game.setGamePaused(true);
    game.setGamePaused(false);
    assertEquals(2, received.getSize());
    assertTrue(received.get(0));
    assertFalse(received.get(1));
  }

  @Test
  void settingTheSameValueDoesNotDispatch() {
    game.setGamePaused(false);
    assertEquals(0, received.getSize());
    game.setGamePaused(true);
    game.setGamePaused(true);
    assertEquals(1, received.getSize());
  }

  @Test
  void theSignalSeesTheGameAlreadyPaused() {
    Flixel.Signals.gamePaused.remove(listener);
    listener = data -> received.add(game.isGamePaused() == data.paused());
    Flixel.Signals.gamePaused.add(listener);
    game.setGamePaused(true);
    game.setGamePaused(false);
    assertTrue(received.get(0));
    assertTrue(received.get(1));
  }

  @Test
  void theSharedConstantsAreDispatched() {
    GamePausedSignalData[] last = new GamePausedSignalData[1];
    SignalHandler<GamePausedSignalData> capture = data -> last[0] = data;
    Flixel.Signals.gamePaused.add(capture);
    game.setGamePaused(true);
    assertSame(GamePausedSignalData.PAUSED, last[0]);
    game.setGamePaused(false);
    assertSame(GamePausedSignalData.RESUMED, last[0]);
    Flixel.Signals.gamePaused.remove(capture);
  }
}
