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
package org.flixelgdx.util.timer;

import org.flixelgdx.GdxHeadlessExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(GdxHeadlessExtension.class)
class FlixelTimerTest {

  private FlixelTimerManager manager;

  @BeforeEach
  void setUp() {
    manager = new FlixelTimerManager();
  }

  // -- Basic start and callback --

  @Test
  void callbackFiresAfterDurationElapses() {
    List<Integer> calls = new ArrayList<>();
    manager.start(1f, t -> calls.add(1), 1);

    manager.update(0.5f);
    assertEquals(0, calls.size(), "callback should not fire before duration");

    manager.update(0.5f);
    assertEquals(1, calls.size(), "callback should fire exactly once after duration");
  }

  @Test
  void callbackDoesNotFireBeforeDurationElapses() {
    List<Integer> calls = new ArrayList<>();
    manager.start(2f, t -> calls.add(1), 1);

    manager.update(1.9f);
    assertTrue(calls.isEmpty());
  }

  @Test
  void timerIsFinishedAfterCallbackFires() {
    List<FlixelTimer> seen = new ArrayList<>();
    manager.start(1f, seen::add, 1);

    manager.update(1f);

    assertEquals(1, seen.size());
    assertTrue(seen.get(0).finished, "timer should be marked finished when callback fires");
    assertFalse(seen.get(0).active, "timer should be inactive when callback fires");
  }

  // -- Overshoot --

  @Test
  void callbackFiresExactlyOnceEvenWhenUpdateOvershootsDuration() {
    List<Integer> calls = new ArrayList<>();
    manager.start(1f, t -> calls.add(1), 1);

    manager.update(10f);
    assertEquals(1, calls.size());
  }

  // -- Cancel --

  @Test
  void cancelPreventsCallbackFromFiring() {
    List<Integer> calls = new ArrayList<>();
    FlixelTimer timer = manager.start(1f, t -> calls.add(1), 1);

    manager.update(0.5f);
    timer.cancel();
    manager.update(1f);

    assertTrue(calls.isEmpty());
  }

  @Test
  void cancelledTimerIsMarkedFinishedAndInactive() {
    FlixelTimer timer = manager.start(1f, t -> {
    }, 1);
    timer.cancel();
    assertTrue(timer.finished);
    assertFalse(timer.active);
  }

  // -- Repeating loops --

  @Test
  void repeatingTimerFiresCorrectNumberOfTimes() {
    List<Integer> calls = new ArrayList<>();
    manager.start(1f, t -> calls.add(1), 3);

    manager.update(1f);
    assertEquals(1, calls.size());

    manager.update(1f);
    assertEquals(2, calls.size());

    manager.update(1f);
    assertEquals(3, calls.size());

    manager.update(1f);
    assertEquals(3, calls.size(), "should not fire again after all loops complete");
  }

  @Test
  void singleLoopTimerDoesNotFireAgainAfterCompletion() {
    List<Integer> calls = new ArrayList<>();
    manager.start(1f, t -> calls.add(1), 1);

    manager.update(1f);
    manager.update(1f);

    assertEquals(1, calls.size());
  }

  // -- Zero-duration --

  @Test
  void zeroDurationTimerFiresOnNextUpdate() {
    List<Integer> calls = new ArrayList<>();
    manager.start(0f, t -> calls.add(1), 1);

    assertEquals(0, calls.size(), "zero-duration timer should not fire until update is called");
    manager.update(0f);
    assertEquals(1, calls.size());
  }

  @Test
  void zeroDurationTimerFiresOnlyOnce() {
    List<Integer> calls = new ArrayList<>();
    manager.start(0f, t -> calls.add(1), 1);

    manager.update(0f);
    manager.update(0f);
    assertEquals(1, calls.size());
  }

  // -- Progress and time-left getters --

  @Test
  void progressIsZeroBeforeAnyUpdate() {
    FlixelTimer timer = manager.start(2f, t -> {
    }, 1);
    assertEquals(0f, timer.getProgress(), 1e-5f);
  }

  @Test
  void progressIsHalfwayAtMidpoint() {
    FlixelTimer timer = manager.start(2f, t -> {
    }, 1);
    manager.update(1f);
    assertEquals(0.5f, timer.getProgress(), 1e-5f);
  }

  @Test
  void timeLeftDecreasesAsUpdateProgresses() {
    FlixelTimer timer = manager.start(2f, t -> {
    }, 1);
    manager.update(1f);
    assertEquals(1f, timer.getTimeLeft(), 1e-5f);
  }

  @Test
  void timeLeftIsZeroAfterCompletion() {
    FlixelTimer timer = manager.start(1f, t -> {
    }, 1);
    manager.update(1f);
    assertEquals(0f, timer.getTimeLeft(), 1e-5f);
  }

  // -- cancelAll --

  @Test
  void cancelAllStopsAllTimersWithoutFiringCallbacks() {
    List<Integer> calls = new ArrayList<>();
    manager.start(1f, t -> calls.add(1), 1);
    manager.start(2f, t -> calls.add(2), 1);

    manager.cancelAll();
    manager.update(10f);

    assertTrue(calls.isEmpty());
  }

  // -- Restart --

  @Test
  void restartBeforeCompletionResetsCountdown() {
    List<Integer> calls = new ArrayList<>();
    // Use 1s and 0.5s steps: 0.5 is exactly representable in IEEE754, so sums are exact.
    FlixelTimer timer = manager.start(1f, t -> calls.add(1), 1);

    manager.update(0.5f);
    assertTrue(calls.isEmpty(), "should not fire at halfway point");

    timer.restart(-1f);  // reset to 1s again

    manager.update(0.5f);
    assertTrue(calls.isEmpty(), "should not fire halfway through restarted timer");

    manager.update(0.5f);
    assertEquals(1, calls.size(), "should fire after full 1s from restart");
  }
}
