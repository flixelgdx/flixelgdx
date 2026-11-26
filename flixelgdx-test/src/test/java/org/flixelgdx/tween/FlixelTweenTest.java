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
package org.flixelgdx.tween;

import org.flixelgdx.FlixelObject;
import org.flixelgdx.tween.ease.FlixelEase;
import org.flixelgdx.tween.settings.FlixelTweenSettings;
import org.flixelgdx.tween.settings.FlixelTweenType;
import org.flixelgdx.tween.type.FlixelColorTween;
import org.flixelgdx.tween.type.FlixelNumTween;
import org.flixelgdx.util.FlixelAxes;
import org.flixelgdx.util.FlixelColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelTweenTest {

  private FlixelTweenManager manager;
  private float value;

  @BeforeEach
  void setUp() {
    TweenTestSupport.resetGlobalTweens();
    manager = TweenTestSupport.newManager();
    value = Float.NaN;
  }

  private FlixelNumTween num(FlixelTweenSettings settings) {
    FlixelNumTween t = manager.obtainTween(FlixelNumTween.class, () -> new FlixelNumTween(0, 0, null, null));
    t.setTweenSettings(settings);
    t.setTarget(0f, 10f, v -> value = v);
    manager.addTween(t);
    return t;
  }

  @Test
  void oneshotReachesTheEndAndIsRemoved() {
    FlixelNumTween t = num(new FlixelTweenSettings().setDuration(1f));
    manager.update(0.25f);
    assertEquals(2.5f, value, 1e-4f);
    manager.update(1f);
    assertEquals(10f, value, 1e-4f);
    assertFalse(manager.getActiveTweens().contains(t, true), "a oneshot tween leaves the manager when it finishes");
  }

  @Test
  void persistStaysInTheManagerAfterFinishing() {
    FlixelNumTween t = num(new FlixelTweenSettings(FlixelTweenType.PERSIST).setDuration(1f));
    manager.update(2f);
    assertTrue(t.isFinished());
    assertFalse(t.getActive());
    assertEquals(1, manager.getActiveTweens().getSize());

    t.restart();
    assertTrue(t.isActive());
    assertFalse(t.getFinished());
    manager.update(0.5f);
    assertEquals(5f, value, 1e-4f);
  }

  @Test
  void backwardRunsFromTheEndToTheStart() {
    num(new FlixelTweenSettings(FlixelTweenType.BACKWARD).setDuration(1f));
    manager.update(0.25f);
    assertEquals(7.5f, value, 1e-4f);
    manager.update(1f);
    assertEquals(0f, value, 1e-4f);
  }

  @Test
  void loopingStartsOverAndCountsCompletions() {
    AtomicInteger completions = new AtomicInteger();
    FlixelNumTween t = num(new FlixelTweenSettings(FlixelTweenType.LOOPING).setDuration(1f)
        .setOnComplete(tw -> completions.incrementAndGet()));
    manager.update(1f);
    assertEquals(1, completions.get());
    assertTrue(t.isActive(), "a looping tween keeps running");
    manager.update(0.5f);
    assertEquals(5f, value, 1e-4f);
  }

  @Test
  void pingpongReversesEachLoop() {
    num(new FlixelTweenSettings(FlixelTweenType.PINGPONG).setDuration(1f));
    manager.update(1f);
    manager.update(0.25f);
    assertEquals(7.5f, value, 1e-4f, "the second run goes back toward the start");
    manager.update(0.75f);
    manager.update(0.25f);
    assertEquals(2.5f, value, 1e-4f, "the third run goes forward again");
  }

  @Test
  void startDelayAndLoopDelayHoldTheTween() {
    num(new FlixelTweenSettings(FlixelTweenType.LOOPING).setDuration(1f).setStartDelay(0.5f).setLoopDelay(0.25f));
    manager.update(0.25f);
    assertTrue(Float.isNaN(value), "nothing happens during the start delay");
    manager.update(0.75f);
    assertEquals(5f, value, 1e-4f);
    manager.update(0.5f);
    assertEquals(10f, value, 1e-4f);
    manager.update(0.1f);
    assertEquals(10f, value, 1e-4f, "the loop delay holds the next loop");
    manager.update(0.65f);
    assertEquals(5f, value, 1e-4f);
  }

  @Test
  void framerateSnapsProgressToSteps() {
    num(new FlixelTweenSettings().setDuration(1f).setFramerate(4f));
    manager.update(0.3f);
    assertEquals(2.5f, value, 1e-4f, "0.3 s rounds to the nearest quarter second");
  }

  @Test
  void easeShapesTheProgress() {
    num(new FlixelTweenSettings().setDuration(1f).setEase(FlixelEase::quadIn));
    manager.update(0.5f);
    assertEquals(2.5f, value, 1e-4f);
  }

  @Test
  void callbacksRunAtTheRightTimes() {
    AtomicInteger starts = new AtomicInteger();
    AtomicInteger updates = new AtomicInteger();
    AtomicInteger completes = new AtomicInteger();
    num(new FlixelTweenSettings().setDuration(1f)
        .setOnStart(t -> starts.incrementAndGet())
        .setOnUpdate(t -> updates.incrementAndGet())
        .setOnComplete(t -> completes.incrementAndGet()));
    manager.update(0.5f);
    manager.update(0f);
    manager.update(1f);
    assertEquals(3, starts.get());
    assertEquals(1, updates.get(), "a zero-length step does not count as an update");
    assertEquals(1, completes.get());
  }

  @Test
  void pauseResumeAndStop() {
    FlixelNumTween t = num(new FlixelTweenSettings().setDuration(1f));
    t.pause();
    manager.update(0.5f);
    assertTrue(Float.isNaN(value));
    t.resume();
    manager.update(0.5f);
    assertEquals(5f, value, 1e-4f);
    t.stop();
    manager.update(0.25f);
    assertEquals(5f, value, 1e-4f);
  }

  @Test
  void aZeroDurationTweenNeverStarts() {
    FlixelNumTween t = num(new FlixelTweenSettings().setDuration(0f));
    assertFalse(t.isActive());
    t.restart();
    assertFalse(t.isActive());
  }

  @Test
  void cancelRemovesTheTween() {
    FlixelNumTween t = num(new FlixelTweenSettings().setDuration(1f));
    t.cancel();
    assertEquals(0, manager.getActiveTweens().getSize());
    assertNull(t.getManager(), "a cancelled tween goes back to its pool and is cleared");
  }

  @Test
  void setManagerMovesTheTween() {
    FlixelNumTween t = num(new FlixelTweenSettings().setDuration(1f));
    FlixelTweenManager other = TweenTestSupport.newManager();
    t.setManager(other);
    assertSame(other, t.getManager());
    assertEquals(1, other.getActiveTweens().getSize());
    assertEquals(0, manager.getActiveTweens().getSize());
  }

  @Test
  void thenStartsTheNextTweenWhenTheFirstFinishes() {
    AtomicInteger order = new AtomicInteger();
    FlixelNumTween first = num(new FlixelTweenSettings().setDuration(1f));
    first.then(() -> {
      order.compareAndSet(0, 1);
      return null;
    }).then(() -> {
      order.compareAndSet(1, 2);
      return num(new FlixelTweenSettings().setDuration(1f));
    });
    manager.update(1f);
    assertEquals(2, order.get(), "chained steps run in order, even when one returns nothing");
  }

  @Test
  void staticNumTweenUsesTheGlobalManager() {
    FlixelTween t = FlixelTween.num(0f, 4f, new FlixelTweenSettings().setDuration(1f), v -> value = v);
    assertSame(FlixelTween.getGlobalManager(), t.getManager());
    FlixelTween.updateTweens(0.5f);
    assertEquals(2f, value, 1e-4f);
  }

  @Test
  void staticGoalTweenMovesAField() {
    FlixelObject o = TweenTestSupport.body();
    FlixelTween.tween(o, new FlixelTweenSettings().setDuration(1f).addGoal(o::getX, 100f, o::setX));
    assertTrue(FlixelTween.containsTweensOf(o));
    FlixelTween.updateTweens(0.5f);
    assertEquals(50f, o.getX(), 1e-3f);
    FlixelTween.completeTweensOf(o);
    assertEquals(100f, o.getX(), 1e-3f);
    assertFalse(FlixelTween.containsTweensOf(o));
  }

  @Test
  void staticGoalTweenRejectsSettingsWithoutGoals() {
    assertThrows(IllegalArgumentException.class, () -> FlixelTween.tween(new Object(), new FlixelTweenSettings()));
    assertThrows(NullPointerException.class, () -> FlixelTween.tween(new Object(), null));
  }

  @Test
  void staticCancelAndCompleteHelpers() {
    TweenTestSupport.Target target = new TweenTestSupport.Target();
    FlixelTween.angle(target, 90f, new FlixelTweenSettings().setDuration(1f));
    FlixelTween.cancelTweensOf(target, "angle");
    assertFalse(FlixelTween.containsTweensOf(target));

    FlixelTween.angle(target, 0f, 90f, new FlixelTweenSettings().setDuration(1f));
    FlixelTween.completeAllTweens();
    assertEquals(90f, target.angle, 1e-4f);

    FlixelTween.color(target, FlixelColor.BLACK, FlixelColor.WHITE, new FlixelTweenSettings().setDuration(1f));
    FlixelTween.completeTweensOfType(FlixelColorTween.class);
    assertEquals(1f, target.color.r, 1e-4f);

    FlixelTween.flicker(target, new FlixelTweenSettings().setDuration(1f));
    FlixelTween.cancelActiveTweens();
    assertEquals(0, FlixelTween.getGlobalManager().getActiveTweens().getSize());
  }

  @Test
  void staticFactoriesCreateEveryTweenType() {
    FlixelObject o = TweenTestSupport.body();
    TweenTestSupport.Target target = new TweenTestSupport.Target();
    FlixelTweenSettings s = new FlixelTweenSettings();
    FlixelTween.shake(target, FlixelAxes.XY, 0.1f, new FlixelTweenSettings());
    FlixelTween.linearMotion(o, 0f, 0f, 10f, 0f, 1f, true, new FlixelTweenSettings());
    FlixelTween.circularMotion(o, 0f, 0f, 5f, 0f, true, 1f, true, new FlixelTweenSettings());
    FlixelTween.quadMotion(o, 0f, 0f, 5f, 5f, 10f, 0f, 1f, true, new FlixelTweenSettings());
    FlixelTween.cubicMotion(o, 0f, 0f, 1f, 1f, 2f, 1f, 3f, 0f, 1f, true, new FlixelTweenSettings());
    FlixelTween.linearPath(o, 1f, true, new FlixelTweenSettings(), 0f, 0f, 10f, 0f);
    FlixelTween.quadPath(o, 1f, true, s, 0f, 0f, 5f, 5f, 10f, 0f);
    assertEquals(7, FlixelTween.getGlobalManager().getActiveTweens().getSize());
    FlixelTween.completeAllTweens();
    assertEquals(10f, o.getX(), 1e-3f);
  }

  @Test
  void pathFactoriesRejectBadPointLists() {
    FlixelTweenSettings s = new FlixelTweenSettings();
    assertThrows(IllegalArgumentException.class, () -> FlixelTween.linearPath(null, 1f, true, s, 0f, 0f));
    assertThrows(IllegalArgumentException.class, () -> FlixelTween.linearPath(null, 1f, true, s, 0f, 0f, 1f));
    assertThrows(IllegalArgumentException.class, () -> FlixelTween.quadPath(null, 1f, true, s, 0f, 0f, 1f, 1f));
    assertThrows(IllegalArgumentException.class,
        () -> FlixelTween.quadPath(null, 1f, true, s, 0f, 0f, 1f, 1f, 2f, 2f, 3f, 3f));
    assertThrows(NullPointerException.class, () -> FlixelTween.linearPath(null, 1f, true, null, 0f, 0f, 1f, 1f));
  }

  @Test
  void clearingPoolsMakesNewTweens() {
    FlixelTween first = FlixelTween.num(0f, 1f, new FlixelTweenSettings().setDuration(1f), v -> {
    });
    FlixelTween.cancelActiveTweens();
    FlixelTween.clearTweenPools();
    FlixelTween second = FlixelTween.num(0f, 1f, new FlixelTweenSettings().setDuration(1f), v -> {
    });
    assertNotSame(first, second);
  }
}
