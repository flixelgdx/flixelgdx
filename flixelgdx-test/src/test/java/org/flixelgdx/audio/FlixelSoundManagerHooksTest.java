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
package org.flixelgdx.audio;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.FlixelState;
import org.flixelgdx.signal.FlixelSignalData.StateSwitchSignalData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlixelSoundManagerHooksTest {

  private RecordingSound sound;
  private FlixelGame game;

  @BeforeEach
  void setUp() {
    sound = new RecordingSound();
    game = new FlixelGame("Sound", TestState::new) {
    };
    Flixel.game = game;
  }

  @Test
  void pausingTheGamePausesAndResumesTheSound() {
    sound.onPaused(true);
    sound.onPaused(false);
    assertEquals("pause resume ", sound.log.toString());
  }

  @Test
  void losingFocusPausesTheSoundOnlyWithAutoPause() {
    Flixel.autoPause = false;
    sound.onFocusLost();
    Flixel.autoPause = true;
    sound.onFocusLost();
    assertEquals("pause ", sound.log.toString());
  }

  @Test
  void regainingFocusResumesTheSoundOnlyWithAutoPause() {
    Flixel.autoPause = false;
    sound.onFocusGained();
    Flixel.autoPause = true;
    sound.onFocusGained();
    assertEquals("resume ", sound.log.toString());
  }

  @Test
  void regainingFocusKeepsTheSoundPausedWhileTheGameIsPaused() {
    Flixel.autoPause = true;
    game.setGamePaused(true);
    sound.onFocusGained();
    game.setGamePaused(false);
    assertEquals("", sound.log.toString());
  }

  @Test
  void aStateSwitchClearsTheSoundsThatDoNotPersist() {
    sound.onStateSwitch(new StateSwitchSignalData(null, false, false));
    assertEquals("clear ", sound.log.toString());
  }

  private static final class RecordingSound extends FlixelSoundManager {

    final StringBuilder log = new StringBuilder();

    RecordingSound() {
      super(FlixelSoundManager.defaultFactory);
    }

    @Override
    public void pause() {
      log.append("pause ");
    }

    @Override
    public void resume() {
      log.append("resume ");
    }

    @Override
    public void clearNonPersist() {
      log.append("clear ");
    }
  }

  private static final class TestState extends FlixelState {
  }
}
