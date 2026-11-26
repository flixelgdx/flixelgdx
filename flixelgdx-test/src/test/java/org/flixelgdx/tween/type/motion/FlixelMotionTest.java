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
package org.flixelgdx.tween.type.motion;

import org.flixelgdx.FlixelObject;
import org.flixelgdx.tween.FlixelTweenManager;
import org.flixelgdx.tween.TweenTestSupport;
import org.flixelgdx.tween.settings.FlixelTweenSettings;
import org.flixelgdx.tween.settings.FlixelTweenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelMotionTest {

  private static final float EPS = 1e-3f;

  private FlixelTweenManager manager;
  private FlixelObject body;

  @BeforeEach
  void setUp() {
    manager = TweenTestSupport.newManager();
    body = TweenTestSupport.body();
  }

  @Test
  void linearMotionMovesInAStraightLine() {
    FlixelTweenSettings s = new FlixelTweenSettings();
    FlixelLinearMotion m = new FlixelLinearMotion(s).setMotion(0f, 0f, 10f, 20f, 2f, true);
    m.setMotionObject(body);
    assertEquals(2f, s.getDuration(), EPS);
    manager.addTween(m);
    manager.update(1f);
    assertEquals(5f, body.getX(), EPS);
    assertEquals(10f, body.getY(), EPS);
    assertEquals(0f, m.getFromX());
    assertEquals(0f, m.getFromY());
    assertEquals(10f, m.getMoveX());
    assertEquals(20f, m.getMoveY());
  }

  @Test
  void speedSetsTheDurationFromTheDistance() {
    FlixelTweenSettings s = new FlixelTweenSettings();
    new FlixelLinearMotion(s).setMotion(0f, 0f, 30f, 40f, 10f, false);
    assertEquals(5f, s.getDuration(), EPS, "50 pixels at 10 pixels per second");

    new FlixelLinearMotion(s).setMotion(0f, 0f, 30f, 40f, 0f, true);
    assertTrue(s.getDuration() > 0f, "a zero duration is raised to a tiny positive one");

    new FlixelLinearMotion(null).setMotion(0f, 0f, 1f, 1f, 1f, true);
  }

  @Test
  void motionLocksTheTargetAndRestoresItWhenDone() {
    body.setImmovable(false);
    FlixelLinearMotion m = new FlixelLinearMotion(new FlixelTweenSettings()).setMotion(0f, 0f, 1f, 0f, 1f, true);
    m.setMotionObject(body);
    assertTrue(body.isImmovable(), "a moving object is locked so physics does not push it");
    assertTrue(m.isImmovableCaptured());
    assertTrue(m.getImmovableCaptured());
    assertFalse(m.isPriorImmovable());
    assertFalse(m.getPriorImmovable());
    assertSame(body, m.getMotionTarget());
    manager.addTween(m);
    manager.update(1f);
    assertFalse(body.isImmovable());
    assertNull(m.getMotionTarget());
  }

  @Test
  void switchingTargetsRestoresTheFirstOne() {
    FlixelObject other = TweenTestSupport.body();
    FlixelLinearMotion m = new FlixelLinearMotion(new FlixelTweenSettings()).setMotion(0f, 0f, 1f, 0f, 1f, true);
    m.setMotionObject(body);
    m.setMotionObject(other);
    assertFalse(body.isImmovable());
    assertTrue(other.isImmovable());
    m.setMotionObject(null);
    assertFalse(other.isImmovable());
    m.reset();
  }

  @Test
  void loopingMotionKeepsItsTarget() {
    FlixelLinearMotion m = new FlixelLinearMotion(new FlixelTweenSettings(FlixelTweenType.LOOPING))
        .setMotion(0f, 0f, 1f, 0f, 1f, true);
    m.setMotionObject(body);
    manager.addTween(m);
    manager.update(1f);
    assertSame(body, m.getMotionTarget());
    assertTrue(body.isImmovable());
  }

  @Test
  void motionMatchesPositionFields() {
    FlixelLinearMotion m = new FlixelLinearMotion(new FlixelTweenSettings());
    assertFalse(m.isTweenOf(body, null));
    m.setMotionObject(body);
    assertTrue(m.isTweenOf(body, null));
    assertTrue(m.isTweenOf(body, ""));
    assertTrue(m.isTweenOf(body, "x"));
    assertTrue(m.isTweenOf(body, "y"));
    assertFalse(m.isTweenOf(body, "angle"));
  }

  @Test
  void motionWithoutATargetStillComputesItsPosition() {
    FlixelLinearMotion m = new FlixelLinearMotion(new FlixelTweenSettings()).setMotion(0f, 0f, 8f, 0f, 1f, true);
    manager.addTween(m);
    manager.update(0.5f);
    assertEquals(4f, m.motionX, EPS);
  }

  @Test
  void circularMotionGoesAroundTheCenter() {
    FlixelTweenSettings s = new FlixelTweenSettings();
    FlixelCircularMotion m = new FlixelCircularMotion(s).setMotion(100f, 100f, 10f, 0f, true, 4f, true);
    assertEquals(110f, m.motionX, EPS);
    assertEquals(100f, m.motionY, EPS);
    m.setMotionObject(body);
    manager.addTween(m);
    manager.update(1f);
    assertEquals(100f, body.getX(), EPS);
    assertEquals(110f, body.getY(), EPS, "a quarter turn clockwise in y-down coordinates");
    assertEquals(100f, m.getCenterX());
    assertEquals(100f, m.getCenterY());
    assertEquals(10f, m.getRadius());
    assertEquals(0f, m.getAngleStartRad(), EPS);
    assertEquals((float) (Math.PI * 2), m.getAngleSweepRad(), EPS);
  }

  @Test
  void counterClockwiseCircleAndSpeed() {
    FlixelTweenSettings s = new FlixelTweenSettings();
    FlixelCircularMotion m =
        new FlixelCircularMotion(s).setMotion(0f, 0f, 10f, 0f, false, 20f * (float) Math.PI, false);
    assertEquals(1f, s.getDuration(), EPS, "one lap of 20 pi pixels at 20 pi pixels per second");
    m.setMotionObject(body);
    manager.addTween(m);
    manager.update(0.25f);
    assertEquals(-10f, body.getY(), EPS);
    assertTrue(m.getAngleSweepRad() < 0f);
  }

  @Test
  void quadMotionFollowsTheCurve() {
    FlixelTweenSettings s = new FlixelTweenSettings();
    FlixelQuadMotion m = new FlixelQuadMotion(s).setMotion(0f, 0f, 10f, 20f, 20f, 0f, 1f, true);
    m.setMotionObject(body);
    manager.addTween(m);
    manager.update(0.5f);
    assertEquals(10f, body.getX(), EPS);
    assertEquals(10f, body.getY(), EPS);
    assertEquals(0f, m.getFromX());
    assertEquals(0f, m.getFromY());
    assertEquals(10f, m.getCx());
    assertEquals(20f, m.getCy());
    assertEquals(20f, m.getToX());
    assertEquals(0f, m.getToY());

    new FlixelQuadMotion(s).setMotion(0f, 0f, 0f, 0f, 30f, 40f, 25f, false);
    assertEquals(2f, s.getDuration(), EPS);
    new FlixelQuadMotion(null).setMotion(0f, 0f, 0f, 0f, 1f, 1f, 1f, true);
  }

  @Test
  void cubicMotionFollowsTheCurve() {
    FlixelTweenSettings s = new FlixelTweenSettings();
    FlixelCubicMotion m = new FlixelCubicMotion(s).setMotion(0f, 0f, 0f, 10f, 10f, 10f, 10f, 0f, 1f, true);
    m.setMotionObject(body);
    manager.addTween(m);
    manager.update(0.5f);
    assertEquals(5f, body.getX(), EPS);
    assertEquals(7.5f, body.getY(), EPS);
    assertEquals(0f, m.getP0x());
    assertEquals(0f, m.getP0y());
    assertEquals(0f, m.getP1x());
    assertEquals(10f, m.getP1y());
    assertEquals(10f, m.getP2x());
    assertEquals(10f, m.getP2y());
    assertEquals(10f, m.getP3x());
    assertEquals(0f, m.getP3y());

    new FlixelCubicMotion(s).setMotion(0f, 0f, 0f, 10f, 10f, 10f, 10f, 0f, 15f, false);
    assertEquals(2f, s.getDuration(), EPS, "30 pixels of control polygon at 15 pixels per second");
    new FlixelCubicMotion(null).setMotion(0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, true);
  }

  @Test
  void linearPathVisitsEveryPointInOrder() {
    FlixelTweenSettings s = new FlixelTweenSettings();
    FlixelLinearPath p = new FlixelLinearPath(s).addPoint(0f, 0f).addPoint(10f, 0f).addPoint(10f, 10f);
    p.setMotion(10f, false);
    assertEquals(2f, s.getDuration(), EPS, "20 pixels at 10 pixels per second");
    p.setMotionObject(body);
    manager.addTween(p);
    assertEquals(0f, p.motionX, EPS);
    manager.update(0.5f);
    assertEquals(5f, body.getX(), EPS);
    assertEquals(0f, body.getY(), EPS);
    manager.update(1f);
    assertEquals(10f, body.getX(), EPS);
    assertEquals(5f, body.getY(), EPS);
  }

  @Test
  void linearPathNeedsTwoPointsAndHandlesZeroLength() {
    assertThrows(IllegalStateException.class, () -> new FlixelLinearPath(null).addPoint(0f, 0f).setMotion(1f, true));
    FlixelLinearPath p = new FlixelLinearPath(new FlixelTweenSettings()).addPoint(3f, 3f).addPoint(3f, 3f);
    p.setMotion(1f, true);
    p.setMotionObject(body);
    manager.addTween(p);
    manager.update(0.5f);
    assertEquals(3f, body.getX(), EPS);
    p.reset();
    manager.update(0.1f);
  }

  @Test
  void linearPathGrowsForManyPoints() {
    FlixelLinearPath p = new FlixelLinearPath(new FlixelTweenSettings());
    for (int i = 0; i <= 20; i++) {
      p.addPoint(i, 0f);
    }
    p.setMotion(1f, true);
    p.setMotionObject(body);
    manager.addTween(p);
    manager.update(0.75f);
    assertEquals(15f, body.getX(), EPS);
  }

  @Test
  void quadPathChainsCurves() {
    FlixelTweenSettings s = new FlixelTweenSettings();
    FlixelQuadPath p = new FlixelQuadPath(s).addPoint(0f, 0f).addPoint(5f, 10f).addPoint(10f, 0f)
        .addPoint(15f, -10f).addPoint(20f, 0f);
    p.setMotion(1f, true);
    p.setMotionObject(body);
    assertEquals(2, p.getNumSegs());
    assertEquals(5, p.getPoints().getSize());
    assertTrue(p.getTotalDistance() > 0f);
    assertEquals(1f, p.getCumulativeT()[2], EPS);
    assertTrue(p.getSegmentLen()[0] > 0f);
    manager.addTween(p);
    assertEquals(0f, p.motionX, EPS);
    manager.update(0.5f);
    assertEquals(10f, body.getX(), EPS, "half way is the shared end point of the two curves");
    manager.update(0.25f);
    assertEquals(15f, body.getX(), EPS);
    assertEquals(-5f, body.getY(), EPS);

    new FlixelQuadPath(s).addPoint(0f, 0f).addPoint(0f, 0f).addPoint(0f, 0f).setMotion(4f, false);
  }

  @Test
  void quadPathRejectsAnEvenPointCount() {
    FlixelQuadPath p = new FlixelQuadPath(null).addPoint(0f, 0f).addPoint(1f, 1f);
    assertThrows(IllegalStateException.class, () -> p.setMotion(1f, true));
    p.addPoint(2f, 2f).addPoint(3f, 3f);
    assertThrows(IllegalStateException.class, () -> p.setMotion(1f, true));
  }

  @Test
  void quadPathGrowsForManyCurvesAndResets() {
    FlixelQuadPath p = new FlixelQuadPath(new FlixelTweenSettings()).addPoint(0f, 0f);
    for (int i = 1; i <= 20; i++) {
      p.addPoint(i * 2 - 1, 0f).addPoint(i * 2, 0f);
    }
    p.setMotion(1f, true);
    assertEquals(20, p.getNumSegs());
    p.reset();
    assertEquals(0, p.getNumSegs());
    manager.addTween(p);
    manager.update(0.5f);
  }

  @Test
  void backwardPathsRunFromTheLastPointToTheFirst() {
    FlixelLinearPath linear = new FlixelLinearPath(new FlixelTweenSettings(FlixelTweenType.BACKWARD))
        .addPoint(0f, 0f).addPoint(10f, 0f);
    linear.setMotion(1f, true);
    linear.setMotionObject(body);
    manager.addTween(linear);
    manager.update(0.25f);
    assertEquals(7.5f, body.getX(), EPS);

    FlixelObject other = TweenTestSupport.body();
    FlixelQuadPath quad = new FlixelQuadPath(new FlixelTweenSettings(FlixelTweenType.BACKWARD))
        .addPoint(0f, 0f).addPoint(10f, 0f).addPoint(20f, 0f);
    quad.setMotion(1f, true);
    quad.setMotionObject(other);
    manager.addTween(quad);
    manager.update(0.25f);
    assertEquals(15f, other.getX(), EPS);
  }

  @Test
  void pingpongPathsComeBackOnTheSecondRun() {
    FlixelLinearPath p = new FlixelLinearPath(new FlixelTweenSettings(FlixelTweenType.PINGPONG))
        .addPoint(0f, 0f).addPoint(10f, 0f);
    p.setMotion(1f, true);
    p.setMotionObject(body);
    manager.addTween(p);
    manager.update(1f);
    assertEquals(10f, body.getX(), EPS);
    manager.update(0.25f);
    assertEquals(7.5f, body.getX(), EPS, "the second run heads back toward the first point");
  }
}
