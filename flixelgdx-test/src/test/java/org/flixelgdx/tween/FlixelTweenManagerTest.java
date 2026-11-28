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

import org.flixelgdx.signal.FlixelSignalData.StateSwitchSignalData;
import org.flixelgdx.tween.settings.FlixelTweenSettings;
import org.flixelgdx.tween.settings.FlixelTweenType;
import org.flixelgdx.tween.type.FlixelAngleTween;
import org.flixelgdx.tween.type.FlixelColorTween;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelTweenManagerTest {

  private FlixelTweenManager manager;
  private TweenTestSupport.Target target;

  @BeforeEach
  void setUp() {
    manager = TweenTestSupport.newManager();
    target = new TweenTestSupport.Target();
  }

  private FlixelAngleTween angle(FlixelTweenType type) {
    FlixelAngleTween t = manager.obtainTween(FlixelAngleTween.class, () -> new FlixelAngleTween(null));
    t.setTweenSettings(new FlixelTweenSettings(type).setDuration(1f));
    t.setAngles(target, 0f, 100f);
    manager.addTween(t);
    return t;
  }

  @Test
  void unregisteredTypesAreRejected() {
    FlixelTweenManager empty = new FlixelTweenManager();
    assertThrows(IllegalArgumentException.class, () -> empty.getPool(FlixelAngleTween.class));
    assertThrows(IllegalArgumentException.class,
        () -> empty.obtainTween(FlixelAngleTween.class, () -> new FlixelAngleTween(null)));
  }

  @Test
  void nullTweensAreIgnored() {
    assertNull(manager.addTween(null));
    assertNull(manager.removeTween(null, true));
  }

  @Test
  void removeWithoutDestroyKeepsTheTweenUsable() {
    FlixelAngleTween t = angle(FlixelTweenType.ONESHOT);
    assertSame(t, manager.removeTween(t, false));
    assertFalse(t.isActive());
    assertSame(manager, t.getManager());
  }

  @Test
  void pooledTweensAreReused() {
    FlixelAngleTween t = angle(FlixelTweenType.ONESHOT);
    manager.update(2f);
    FlixelAngleTween again = manager.obtainTween(FlixelAngleTween.class, () -> new FlixelAngleTween(null));
    assertSame(t, again, "a finished oneshot tween goes back to the pool");
    manager.clearPools();
    assertFalse(manager.obtainTween(FlixelAngleTween.class, () -> new FlixelAngleTween(null)) == t);
  }

  @Test
  void containsCancelAndCompleteMatchFieldNames() {
    angle(FlixelTweenType.ONESHOT);
    assertTrue(manager.containsTweensOf(target));
    assertTrue(manager.containsTweensOf(target, "angle"));
    assertTrue(manager.containsTweensOf(target, "color", null));
    assertFalse(manager.containsTweensOf(target, "color"));
    assertFalse(manager.containsTweensOf(new Object()));

    manager.cancelTweensOf(target, "color");
    assertTrue(manager.containsTweensOf(target));
    manager.completeTweensOf(target, "angle");
    assertEquals(100f, target.angle, 1e-4f);
    assertFalse(manager.containsTweensOf(target));
  }

  @Test
  void completeSkipsLoopingAndInactiveTweens() {
    FlixelAngleTween looping = angle(FlixelTweenType.LOOPING);
    FlixelAngleTween paused = angle(FlixelTweenType.ONESHOT);
    paused.setActive(false);
    manager.completeAll();
    manager.completeTweensOf(target);
    manager.completeTweensOfType(FlixelAngleTween.class);
    assertTrue(looping.isActive());
    assertFalse(looping.isFinished());
    assertEquals(2, manager.getActiveTweens().getSize());

    manager.cancelTweensOf(target);
    assertEquals(1, manager.getActiveTweens().getSize(), "the inactive tween is not cancelled");
  }

  @Test
  void completeOfTypeOnlyTouchesThatType() {
    angle(FlixelTweenType.ONESHOT);
    manager.completeTweensOfType(FlixelColorTween.class);
    assertEquals(1, manager.getActiveTweens().getSize());
    manager.completeTweensOfType(FlixelAngleTween.class);
    assertEquals(0, manager.getActiveTweens().getSize());
  }

  @Test
  void nullArgumentsAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> manager.cancelTweensOf(null));
    assertThrows(IllegalArgumentException.class, () -> manager.completeTweensOf(null));
    assertThrows(IllegalArgumentException.class, () -> manager.completeTweensOfType(null));
    assertThrows(IllegalArgumentException.class, () -> manager.containsTweensOf(null));
    assertThrows(IllegalArgumentException.class, () -> manager.forEach(null));
  }

  @Test
  void forEachVisitsEveryTween() {
    angle(FlixelTweenType.ONESHOT);
    angle(FlixelTweenType.PERSIST);
    AtomicInteger count = new AtomicInteger();
    manager.forEach(t -> count.incrementAndGet());
    assertEquals(2, count.get());
  }

  @Test
  void aTweenOwnedByAnotherManagerIsNotFinishedHere() {
    FlixelTweenManager other = TweenTestSupport.newManager();
    FlixelAngleTween t = other.obtainTween(FlixelAngleTween.class, () -> new FlixelAngleTween(null));
    t.setTweenSettings(new FlixelTweenSettings().setDuration(1f));
    t.setAngles(target, 0f, 100f);
    other.addTween(t);
    manager.getActiveTweens().add(t);
    t.finished = true;
    manager.update(0f);
    assertTrue(manager.getActiveTweens().contains(t, true), "only the owning manager finishes a tween");
    assertTrue(t.isActive());
  }

  @Test
  void resetRegistryForgetsEverything() {
    angle(FlixelTweenType.ONESHOT);
    manager.resetRegistry();
    assertEquals(0, manager.getActiveTweens().getSize());
    assertThrows(IllegalArgumentException.class, () -> manager.getPool(FlixelAngleTween.class));
  }

  @Test
  void cancelAllCancelsEveryActiveTween() {
    angle(FlixelTweenType.ONESHOT);
    angle(FlixelTweenType.LOOPING);
    manager.cancelAll();
    assertEquals(0, manager.getActiveTweens().getSize());
  }

  @Test
  void aStateSwitchClearsTheTweensOnlyWhenAsked() {
    angle(FlixelTweenType.ONESHOT);
    manager.onStateSwitch(new StateSwitchSignalData(null, false, true));
    assertEquals(1, manager.getActiveTweens().getSize());
    manager.onStateSwitch(new StateSwitchSignalData(null, true, false));
    assertEquals(0, manager.getActiveTweens().getSize());
  }

  @Test
  void destroyCancelsEveryTweenAndClearsTheRegistry() {
    angle(FlixelTweenType.ONESHOT);
    manager.destroy();
    assertEquals(0, manager.getActiveTweens().getSize());
    assertThrows(IllegalArgumentException.class, () -> manager.getPool(FlixelAngleTween.class));
  }
}
