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
package org.flixelgdx.timer;

import org.flixelgdx.signal.FlixelSignalData.StateSwitchSignalData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelTimerManagerHooksTest {

  @Test
  void aStateSwitchCancelsTheTimersOnlyWhenAsked() {
    FlixelTimerManager manager = new FlixelTimerManager();
    FlixelTimer timer = manager.start(5f, null);
    manager.onStateSwitch(new StateSwitchSignalData(null, true, false));
    assertTrue(timer.active);
    manager.onStateSwitch(new StateSwitchSignalData(null, false, true));
    assertTrue(timer.finished);
  }

  @Test
  void destroyingTheGlobalManagerOnlyCancelsItsTimers() {
    FlixelTimerManager global = FlixelTimer.getGlobalManager();
    FlixelTimer timer = FlixelTimer.wait(5f, t -> {
    });
    global.destroy();
    assertTrue(timer.finished);
    assertTrue(global.isExists());
    FlixelTimer next = FlixelTimer.wait(1f, t -> {
    });
    global.update(2f);
    assertTrue(next.finished);
  }

  @Test
  void destroyingALocalManagerMarksItAsDestroyed() {
    FlixelTimerManager manager = new FlixelTimerManager();
    manager.start(5f, null);
    manager.destroy();
    assertFalse(manager.isExists());
  }
}
