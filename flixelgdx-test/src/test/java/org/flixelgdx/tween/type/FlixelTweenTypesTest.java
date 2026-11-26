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
package org.flixelgdx.tween.type;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.tween.FlixelTweenManager;
import org.flixelgdx.tween.TweenTestSupport;
import org.flixelgdx.tween.TweenTestSupport.Target;
import org.flixelgdx.tween.settings.FlixelShakeUnit;
import org.flixelgdx.tween.settings.FlixelTweenSettings;
import org.flixelgdx.tween.settings.FlixelTweenType;
import org.flixelgdx.util.FlixelAxes;
import org.flixelgdx.util.FlixelColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelTweenTypesTest {

  private FlixelTweenManager manager;
  private Target target;

  @BeforeEach
  void setUp() {
    manager = TweenTestSupport.newManager();
    target = new Target();
    Flixel.random.setSeed(1234L);
  }

  private static FlixelTweenSettings oneSecond() {
    return new FlixelTweenSettings().setDuration(1f);
  }

  @Test
  void goalTweenMovesEveryGoalFromItsStartValue() {
    FlixelObject o = new FlixelObject(10f, 20f, 0f, 0f);
    FlixelGoalTween t = new FlixelGoalTween(oneSecond().addGoal(o::getX, 30f, o::setX).addGoal(o::getY, 0f, o::setY));
    t.setObject(o).setFieldLabel("x");
    manager.addTween(t);
    manager.update(0.5f);
    assertEquals(20f, o.getX(), 1e-4f);
    assertEquals(10f, o.getY(), 1e-4f);
    assertSame(o, t.getTweenObject());
    assertEquals("x", t.getFieldLabel());
  }

  @Test
  void goalTweenNeedsAnObject() {
    FlixelGoalTween t = new FlixelGoalTween(oneSecond());
    assertThrows(IllegalStateException.class, t::start);
  }

  @Test
  void goalTweenWithoutGoalsDoesNothing() {
    FlixelGoalTween t = new FlixelGoalTween(oneSecond());
    t.setObject(new Object());
    manager.addTween(t);
    manager.update(0.5f);
    assertTrue(t.isActive());
  }

  @Test
  void goalTweenRestartReadsTheStartValuesAgain() {
    FlixelObject o = new FlixelObject(0f, 0f, 0f, 0f);
    FlixelGoalTween t = new FlixelGoalTween(new FlixelTweenSettings(FlixelTweenType.PERSIST).setDuration(1f)
        .addGoal(o::getX, 10f, o::setX));
    t.setObject(o);
    manager.addTween(t);
    manager.update(1f);
    o.setX(50f);
    t.restart();
    manager.update(0.5f);
    assertEquals(30f, o.getX(), 1e-4f, "the second run starts from the value at restart time");
  }

  @Test
  void goalTweenMatchesFieldPaths() {
    Object o = new Object();
    FlixelGoalTween t = new FlixelGoalTween(oneSecond());
    assertFalse(t.isTweenOf(o, null), "no object yet");
    t.setObject(o);
    assertTrue(t.isTweenOf(o, null));
    assertTrue(t.isTweenOf(o, ""));
    assertTrue(t.isTweenOf(o, "x"), "without a label every field matches");
    t.setFieldLabel("x");
    assertTrue(t.isTweenOf(o, "x"));
    assertFalse(t.isTweenOf(o, "y"));
    assertTrue(t.isTweenOf(o, "player.position.x"));
    assertFalse(t.isTweenOf(o, "player.position.y"));
    assertFalse(t.isTweenOf(new Object(), "x"));

    t.reset();
    assertNull(t.getTweenObject());
    assertNull(t.getFieldLabel());
  }

  @Test
  void numTweenWithoutACallbackIsSafe() {
    FlixelNumTween t = new FlixelNumTween(0f, 1f, oneSecond(), null);
    manager.addTween(t);
    manager.update(0.5f);
    assertFalse(t.isTweenOf(new Object(), null));
  }

  @Test
  void angleTweenStartsFromTheCurrentAngleWhenNoStartIsGiven() {
    target.angle = 40f;
    FlixelAngleTween t = new FlixelAngleTween(oneSecond()).setAngles(target, Float.NaN, 80f);
    manager.addTween(t);
    manager.update(0.5f);
    assertEquals(60f, target.angle, 1e-4f);
    assertTrue(t.isTweenOf(target, "angle"));
    assertTrue(t.isTweenOf(target, null));
    assertFalse(t.isTweenOf(target, "color"));
    t.reset();
    assertFalse(t.isTweenOf(target, null));
  }

  @Test
  void angleTweenWithoutATargetIsSafe() {
    FlixelAngleTween t = new FlixelAngleTween(oneSecond()).setAngles(null, 0f, 10f);
    manager.addTween(t);
    manager.update(0.5f);
    assertFalse(t.isTweenOf(target, null));
  }

  @Test
  void colorTweenBlendsBetweenTwoColors() {
    AtomicInteger calls = new AtomicInteger();
    FlixelColorTween t = new FlixelColorTween(oneSecond())
        .setColorEndpoints(target, FlixelColor.BLACK, FlixelColor.WHITE, calls::incrementAndGet);
    manager.addTween(t);
    manager.update(0.5f);
    assertEquals(0.5f, target.color.r, 1e-3f);
    assertEquals(1, calls.get());
    assertTrue(t.isTweenOf(target, "color"));
    assertTrue(t.isTweenOf(target, ""));
    assertFalse(t.isTweenOf(target, "angle"));
  }

  @Test
  void rawColorTweenCopiesItsEndpoints() {
    FlixelColor from = new FlixelColor(FlixelColor.BLACK);
    FlixelColorTween t = new FlixelColorTween(oneSecond()).setColorEndpointsRaw(target, from, FlixelColor.RED, null);
    from.set(FlixelColor.WHITE);
    manager.addTween(t);
    manager.update(0.5f);
    assertEquals(0.5f, target.color.r, 1e-3f, "changing the caller's color afterwards has no effect");
    assertEquals(0f, target.color.g, 1e-3f);
  }

  @Test
  void colorTweenWithMissingEndpointsOrTargetIsSafe() {
    FlixelColorTween t = new FlixelColorTween(oneSecond()).setColorEndpoints(target, null, FlixelColor.RED, null);
    manager.addTween(t);
    manager.update(0.5f);
    assertEquals(1f, target.color.g, 1e-3f, "nothing changes without a start color");

    FlixelColorTween noTarget = new FlixelColorTween(oneSecond()).setColorEndpointsRaw(null, null, null, null);
    manager.addTween(noTarget);
    manager.update(0.25f);
    assertFalse(noTarget.isTweenOf(target, null));
    t.reset();
    assertFalse(t.isTweenOf(target, null));
  }

  @Test
  void shakeStaysInsideTheRangeAndRestoresTheStartOffset() {
    target.shakeX = 5f;
    target.shakeY = 7f;
    FlixelShakeTween t = new FlixelShakeTween(oneSecond()).setShake(target, FlixelAxes.XY, 2f)
        .setShakeUnit(FlixelShakeUnit.PIXELS);
    manager.addTween(t);
    assertEquals(FlixelShakeUnit.PIXELS, t.getShakeUnit());
    for (int i = 0; i < 20; i++) {
      manager.update(0.01f);
      assertTrue(Math.abs(target.shakeX - 5f) <= 2f);
      assertTrue(Math.abs(target.shakeY - 7f) <= 2f);
    }
    t.cancel();
    assertEquals(5f, target.shakeX);
    assertEquals(7f, target.shakeY);
  }

  @Test
  void shakeOnOneAxisLeavesTheOtherAlone() {
    FlixelShakeTween x = new FlixelShakeTween(oneSecond()).setShake(target, FlixelAxes.X, 3f);
    x.setShakeUnit(FlixelShakeUnit.PIXELS);
    manager.addTween(x);
    manager.update(0.1f);
    assertEquals(0f, target.shakeY);
    x.cancel();

    FlixelShakeTween y = new FlixelShakeTween(oneSecond()).setShake(target, FlixelAxes.Y, 3f);
    manager.addTween(y);
    manager.update(0.1f);
    assertEquals(0f, target.shakeX);
  }

  @Test
  void fractionShakeScalesWithTheTargetSize() {
    target.shakeWidth = 100f;
    target.shakeHeight = 50f;
    FlixelShakeTween t = new FlixelShakeTween(oneSecond()).setShake(target, null, 0.1f).setShakeUnit(null);
    assertEquals(FlixelShakeUnit.FRACTION, t.getShakeUnit());
    manager.addTween(t);
    float maxX = 0f;
    float maxY = 0f;
    for (int i = 0; i < 50; i++) {
      manager.update(0.01f);
      maxX = Math.max(maxX, Math.abs(target.shakeX));
      maxY = Math.max(maxY, Math.abs(target.shakeY));
    }
    assertTrue(maxX <= 10f && maxX > 1f, "x range is 10% of the width");
    assertTrue(maxY <= 5f, "y range is 10% of the height");
  }

  @Test
  void fractionShakeWithoutASizeUsesTheIntensityAsPixels() {
    FlixelShakeTween t = new FlixelShakeTween(oneSecond()).setShake(target, FlixelAxes.XY, 0.5f);
    manager.addTween(t);
    for (int i = 0; i < 20; i++) {
      manager.update(0.01f);
      assertTrue(Math.abs(target.shakeX) <= 0.5f);
      assertTrue(Math.abs(target.shakeY) <= 0.5f);
    }
  }

  @Test
  void fadingShakeSettlesAtTheEnd() {
    FlixelShakeTween t = new FlixelShakeTween(oneSecond()).setShake(target, FlixelAxes.XY, 4f).setFadeOut(true);
    t.setShakeUnit(FlixelShakeUnit.PIXELS);
    assertTrue(t.isFadeOut());
    assertTrue(t.getFadeOut());
    manager.addTween(t);
    manager.update(0.999f);
    assertTrue(Math.abs(target.shakeX) < 0.01f, "almost no shake is left near the end");
  }

  @Test
  void shakeMatchesItsTargetAndResets() {
    FlixelShakeTween t = new FlixelShakeTween(oneSecond());
    assertFalse(t.isTweenOf(target, null));
    t.setShake(target, FlixelAxes.XY, 1f);
    assertTrue(t.isTweenOf(target, "shake"));
    assertTrue(t.isTweenOf(target, null));
    assertFalse(t.isTweenOf(target, "x"));
    manager.addTween(t);
    manager.update(0.5f);
    t.reset();
    assertEquals(0f, target.shakeX);
    assertFalse(t.isFadeOut());
    assertEquals(FlixelShakeUnit.FRACTION, t.getShakeUnit());

    FlixelShakeTween none = new FlixelShakeTween(oneSecond()).setShake(null, FlixelAxes.XY, 1f);
    manager.addTween(none);
    manager.update(0.5f);
    none.cancel();
  }

  @Test
  void flickerTogglesVisibilityAndEndsOnTheChosenState() {
    FlixelFlickerTween t = new FlixelFlickerTween(oneSecond()).setFlicker(target, 0.1f, 0.5f, false, null);
    assertEquals(0.1f, t.getPeriod(), 1e-6f);
    assertEquals(0.5f, t.getRatio(), 1e-6f);
    manager.addTween(t);
    manager.update(0.07f);
    assertTrue(target.visible, "phase 0.7 is past the 0.5 ratio");
    manager.update(0.05f);
    assertFalse(target.visible);
    manager.update(1f);
    assertFalse(target.visible, "a oneshot flicker ends on endVisibility");
  }

  @Test
  void flickerClampsItsSettings() {
    FlixelFlickerTween t = new FlixelFlickerTween(oneSecond()).setFlicker(target, 0f, 2f, true, null);
    assertEquals(1f / 60f, t.getPeriod(), 1e-6f);
    assertEquals(1f, t.getRatio(), 1e-6f);
    t.setFlicker(target, 1f, -1f, true, null);
    assertEquals(0f, t.getRatio(), 1e-6f);
  }

  @Test
  void flickerUsesACustomFunctionAndWaitsForTheDelay() {
    FlixelFlickerTween t = new FlixelFlickerTween(oneSecond().setStartDelay(0.5f))
        .setFlicker(target, 0.1f, 0.5f, true, tw -> tw.getSecondsSinceStart() < 0.8f);
    manager.addTween(t);
    manager.update(0.6f);
    assertTrue(target.visible);
    manager.update(0.3f);
    assertFalse(target.visible);
  }

  @Test
  void loopingFlickerDoesNotForceTheEndState() {
    target.visible = true;
    FlixelFlickerTween t = new FlixelFlickerTween(new FlixelTweenSettings(FlixelTweenType.LOOPING).setDuration(0.1f))
        .setFlicker(target, 0.04f, 0.5f, true, tw -> false);
    manager.addTween(t);
    manager.update(0.1f);
    assertFalse(target.visible, "looping flickers keep their last state");
  }

  @Test
  void flickerMatchesItsTargetAndResets() {
    FlixelFlickerTween t = new FlixelFlickerTween(oneSecond());
    assertFalse(t.isTweenOf(target, null));
    t.setFlicker(target, 0.1f, 0.5f, true, null);
    assertTrue(t.isTweenOf(target, "visible"));
    assertTrue(t.isTweenOf(target, "flicker"));
    assertTrue(t.isTweenOf(target, ""));
    assertFalse(t.isTweenOf(target, "alpha"));
    t.reset();
    assertEquals(0.08f, t.getPeriod(), 1e-6f);

    FlixelFlickerTween none = new FlixelFlickerTween(oneSecond()).setFlicker(null, 0.1f, 0.5f, true, null);
    manager.addTween(none);
    manager.update(2f);
  }

  @Test
  void defaultFlickerFunctionHandlesAZeroPeriod() {
    FlixelFlickerTween t = new FlixelFlickerTween(oneSecond());
    t.setFlicker(target, 0.1f, 0.5f, true, null);
    t.period = 0f;
    assertFalse(FlixelFlickerTween.defaultTweenFunction(t));
  }
}
