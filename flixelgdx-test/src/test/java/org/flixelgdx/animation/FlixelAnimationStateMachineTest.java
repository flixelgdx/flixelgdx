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
package org.flixelgdx.animation;

import com.badlogic.gdx.utils.Array;

import org.flixelgdx.FlixelSprite;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link FlixelAnimationStateMachine} transition logic, legality rules, lifecycle hooks,
 * and auto-advance.
 *
 * <p>No clips are registered on the controller, so {@code playAnimation} is a no-op here; that is
 * fine because every behavior under test is driven by the machine's own state, signals, and the
 * controller's {@code onAnimationFinished} signal (which the tests dispatch manually to simulate a
 * clip ending). That keeps the suite free of any GPU texture.
 */
class FlixelAnimationStateMachineTest {

  @Test
  void transitionsUpdateStateAndFireSignalOnce() {
    FlixelAnimationStateMachine fsm = new FlixelAnimationStateMachine(new FlixelSprite());
    fsm.addState("idle", "idle");
    fsm.addState("run", "run");

    Array<String> seen = new Array<>();
    fsm.onStateChanged.add(seen::add);

    assertTrue(fsm.setState("idle"));
    assertEquals("idle", fsm.getState());

    // Re-entering the active state is a no-op: no change, no extra signal.
    assertTrue(fsm.setState("idle"));
    assertTrue(fsm.setState("run"));

    assertEquals("run", fsm.getState());
    assertEquals(2, seen.size, "Signal should fire only on real state changes.");
    assertEquals("idle", seen.get(0));
    assertEquals("run", seen.get(1));
  }

  @Test
  void illegalTransitionsAreRejectedButLegalOnesPass() {
    FlixelAnimationStateMachine fsm = new FlixelAnimationStateMachine(new FlixelSprite());
    fsm.addState("idle", "idle").allowTo("run");
    fsm.addState("run", "run");
    fsm.addState("hurt", "hurt");
    fsm.setState("idle");

    // "idle" only allows "run", so jumping straight to "hurt" must be refused.
    assertFalse(fsm.setState("hurt"));
    assertEquals("idle", fsm.getState());

    assertTrue(fsm.setState("run"));
    assertEquals("run", fsm.getState());

    // "run" declared no restrictions, so any target is allowed from it.
    assertTrue(fsm.setState("hurt"));
    assertEquals("hurt", fsm.getState());
  }

  @Test
  void enterAndExitHooksRunInOrder() {
    FlixelAnimationStateMachine fsm = new FlixelAnimationStateMachine(new FlixelSprite());
    Array<String> log = new Array<>();
    fsm.addState("idle", "idle").onExit(() -> log.add("exit-idle"));
    fsm.addState("run", "run").onEnter(() -> log.add("enter-run"));

    fsm.setState("idle");
    fsm.setState("run");

    assertEquals(2, log.size);
    assertEquals("exit-idle", log.get(0), "The old state must exit before the new one enters.");
    assertEquals("enter-run", log.get(1));
  }

  @Test
  void forcedReentryReplaysWithoutExiting() {
    FlixelAnimationStateMachine fsm = new FlixelAnimationStateMachine(new FlixelSprite());
    int[] enters = {0};
    int[] exits = {0};
    fsm.addState("attack", "attack").onEnter(() -> enters[0]++).onExit(() -> exits[0]++);

    fsm.setState("attack");
    fsm.setState("attack", true); // forced re-entry

    assertEquals(2, enters[0], "A forced re-entry should run onEnter again.");
    assertEquals(0, exits[0], "A forced re-entry never leaves the state, so onExit must not run.");
  }

  @Test
  void autoAdvanceFollowsWhenTheClipFinishes() {
    FlixelSprite sprite = new FlixelSprite();
    FlixelAnimationStateMachine fsm = new FlixelAnimationStateMachine(sprite);
    fsm.addState("idle", "idle");
    fsm.addState("attack", "attack").autoAdvanceTo("idle");

    fsm.setState("attack");
    assertEquals("attack", fsm.getState());

    // Simulate the controller reporting that the "attack" clip finished.
    sprite.ensureAnimation().onAnimationFinished
        .dispatch(new FlixelAnimationFrameSignalData("attack", 0, null));

    assertEquals("idle", fsm.getState(), "The one-shot clip finishing should advance to idle.");
  }

  @Test
  void autoAdvanceIgnoresAnUnrelatedClipFinishing() {
    FlixelSprite sprite = new FlixelSprite();
    FlixelAnimationStateMachine fsm = new FlixelAnimationStateMachine(sprite);
    fsm.addState("idle", "idle");
    fsm.addState("attack", "attack").autoAdvanceTo("idle");
    fsm.setState("attack");

    // A different clip ending must not bounce the machine forward.
    sprite.ensureAnimation().onAnimationFinished
        .dispatch(new FlixelAnimationFrameSignalData("somethingElse", 0, null));

    assertEquals("attack", fsm.getState());
  }

  @Test
  void destroyStopsDrivingAutoAdvance() {
    FlixelSprite sprite = new FlixelSprite();
    FlixelAnimationStateMachine fsm = new FlixelAnimationStateMachine(sprite);
    fsm.addState("idle", "idle");
    fsm.addState("attack", "attack").autoAdvanceTo("idle");
    fsm.setState("attack");

    fsm.destroy();

    // After destroy the machine is detached, so a finished clip must not advance it. destroy() also
    // resets the state to empty, so the tell-tale of a leak would be the state becoming "idle".
    sprite.ensureAnimation().onAnimationFinished
        .dispatch(new FlixelAnimationFrameSignalData("attack", 0, null));

    assertEquals("", fsm.getState(), "A detached machine must not auto-advance to idle.");
    assertFalse(fsm.hasState("idle"), "destroy() should clear registered states.");
  }
}
