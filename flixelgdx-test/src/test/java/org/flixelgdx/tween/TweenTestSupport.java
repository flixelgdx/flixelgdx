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
import org.flixelgdx.functional.FlixelAngleable;
import org.flixelgdx.functional.FlixelColorable;
import org.flixelgdx.functional.FlixelShakeable;
import org.flixelgdx.functional.FlixelVisible;
import org.flixelgdx.tween.type.FlixelAngleTween;
import org.flixelgdx.tween.type.FlixelColorTween;
import org.flixelgdx.tween.type.FlixelFlickerTween;
import org.flixelgdx.tween.type.FlixelGoalTween;
import org.flixelgdx.tween.type.FlixelNumTween;
import org.flixelgdx.tween.type.FlixelShakeTween;
import org.flixelgdx.tween.type.motion.FlixelCircularMotion;
import org.flixelgdx.tween.type.motion.FlixelCubicMotion;
import org.flixelgdx.tween.type.motion.FlixelLinearMotion;
import org.flixelgdx.tween.type.motion.FlixelLinearPath;
import org.flixelgdx.tween.type.motion.FlixelQuadMotion;
import org.flixelgdx.tween.type.motion.FlixelQuadPath;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;

public final class TweenTestSupport {

  private TweenTestSupport() {}

  public static void resetGlobalTweens() {
    FlixelTween.resetRegistry();
    FlixelTween.registerTweenType(FlixelGoalTween.class, () -> new FlixelGoalTween(null))
        .registerTweenType(FlixelNumTween.class, () -> new FlixelNumTween(0, 0, null, null))
        .registerTweenType(FlixelAngleTween.class, () -> new FlixelAngleTween(null))
        .registerTweenType(FlixelColorTween.class, () -> new FlixelColorTween(null))
        .registerTweenType(FlixelShakeTween.class, () -> new FlixelShakeTween(null))
        .registerTweenType(FlixelFlickerTween.class, () -> new FlixelFlickerTween(null))
        .registerTweenType(FlixelLinearMotion.class, () -> new FlixelLinearMotion(null))
        .registerTweenType(FlixelCircularMotion.class, () -> new FlixelCircularMotion(null))
        .registerTweenType(FlixelQuadMotion.class, () -> new FlixelQuadMotion(null))
        .registerTweenType(FlixelCubicMotion.class, () -> new FlixelCubicMotion(null))
        .registerTweenType(FlixelLinearPath.class, () -> new FlixelLinearPath(null))
        .registerTweenType(FlixelQuadPath.class, () -> new FlixelQuadPath(null));
  }

  public static FlixelTweenManager newManager() {
    FlixelTweenManager m = new FlixelTweenManager();
    m.registerTweenType(FlixelGoalTween.class, () -> new FlixelGoalTween(null));
    m.registerTweenType(FlixelNumTween.class, () -> new FlixelNumTween(0, 0, null, null));
    m.registerTweenType(FlixelAngleTween.class, () -> new FlixelAngleTween(null));
    m.registerTweenType(FlixelColorTween.class, () -> new FlixelColorTween(null));
    m.registerTweenType(FlixelShakeTween.class, () -> new FlixelShakeTween(null));
    m.registerTweenType(FlixelFlickerTween.class, () -> new FlixelFlickerTween(null));
    m.registerTweenType(FlixelLinearMotion.class, () -> new FlixelLinearMotion(null));
    m.registerTweenType(FlixelCircularMotion.class, () -> new FlixelCircularMotion(null));
    m.registerTweenType(FlixelQuadMotion.class, () -> new FlixelQuadMotion(null));
    m.registerTweenType(FlixelCubicMotion.class, () -> new FlixelCubicMotion(null));
    m.registerTweenType(FlixelLinearPath.class, () -> new FlixelLinearPath(null));
    m.registerTweenType(FlixelQuadPath.class, () -> new FlixelQuadPath(null));
    return m;
  }

  public static FlixelObject body() {
    return new FlixelObject(0f, 0f, 0f, 0f);
  }

  public static final class Target implements FlixelAngleable, FlixelColorable, FlixelVisible, FlixelShakeable {

    public final FlixelColor color = new FlixelColor(FlixelColor.WHITE);
    public float angle;
    public float shakeX;
    public float shakeY;
    public float shakeWidth;
    public float shakeHeight;
    public int visibilityChanges;

    public boolean visible = true;

    @Override
    public float getAngle() {
      return angle;
    }

    @Override
    public void setAngle(float degrees) {
      angle = degrees;
    }

    @Override
    public void changeAngle(float deltaDegrees) {
      angle += deltaDegrees;
    }

    @Override
    public FlixelColor getColor() {
      return color;
    }

    @Override
    public void setColor(@NotNull FlixelColor c) {
      color.set(c);
    }

    @Override
    public boolean isVisible() {
      return visible;
    }

    @Override
    public void setVisible(boolean v) {
      visible = v;
      visibilityChanges++;
    }

    @Override
    public void toggleVisible() {
      setVisible(!visible);
    }

    @Override
    public float getShakeX() {
      return shakeX;
    }

    @Override
    public float getShakeY() {
      return shakeY;
    }

    @Override
    public void setShake(float x, float y) {
      shakeX = x;
      shakeY = y;
    }

    @Override
    public float getShakeWidth() {
      return shakeWidth;
    }

    @Override
    public float getShakeHeight() {
      return shakeHeight;
    }
  }
}
