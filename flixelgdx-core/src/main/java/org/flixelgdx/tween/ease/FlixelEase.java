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
package org.flixelgdx.tween.ease;

import org.flixelgdx.tween.FlixelTween;
import org.flixelgdx.tween.settings.FlixelTweenSettings;

/**
 * The framework's default easer functions, used for tweening to add visual animations.
 *
 * <p>Almost every easer typically has three main types: {@code *In}, {@code *Out}, and
 * {@code *InOut}. {@code *In} means the <i>first</i> half of the ease, {@code *Out}
 * means the <i>second</i> half, and {@code *InOut} is both halves combined.
 *
 * <p>You can think of easers and their rate of change like walking over a hill:
 * <ul>
 *   <li>
 *     <b>{@code *In}</b> - You walk <i>up</i> the hill: you start a little gentle at first,
 *     but you gradually put more effort as you go up.
 *   </li>
 *   <li>
 *     <b>{@code *Out}</b> - You walk <i>down</i> the hill: you start fast because gravity
 *     helps, but gradually slow down as you reach the bottom.
 *   </li>
 *   <li>
 *     <b>{@code *InOut}</b> - You walk <i>up</i> the hill, reach the peak, then start descending
 *     <i>down</i> the hill and gradually slow down.
 *   </li>
 * </ul>
 *
 * <p>Note that every easer always covers the same distance in the same time. The only
 * difference is <i>how</i> they handle it.
 *
 * <p>You typically pass these functions down as lambda expressions for a {@link FlixelTweenSettings}
 * object when using them in the framework's tweening engine, like so:
 *
 * <pre>{@code
 * FlixelTween.tween(myObject, new FlixelTweenSettings()
 *     .setEase(FlixelEase::quadOut));
 * }</pre>
 *
 * @see FlixelTween
 * @see FlixelTweenSettings
 * @author stringdotjar
 */
public final class FlixelEase {

  // Easing constants for specific functions.
  private static final float PI2 = (float) Math.PI / 2;
  private static final float B1 = (float) ((float) 1 / 2.75);
  private static final float B2 = (float) ((float) 2 / 2.75);
  private static final float B3 = (float) ((float) 1.5 / 2.75);
  private static final float B4 = (float) ((float) 2.5 / 2.75);
  private static final float B5 = (float) ((float) 2.25 / 2.75);
  private static final float B6 = (float) ((float) 2.625 / 2.75);
  private static final float ELASTIC_AMPLITUDE = 1;
  private static final float ELASTIC_PERIOD = 0.4f;

  private FlixelEase() {}

  /**
   * Applies linear easing, with a constant rate of change throughout.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float linear(float t) {
    return t;
  }

  /**
   * Applies quadratic ease-in, accelerating slowly from zero.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float quadIn(float t) {
    return t * t;
  }

  /**
   * Applies quadratic ease-out, decelerating smoothly to zero.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float quadOut(float t) {
    return -t * (t - 2);
  }

  /**
   * Applies quadratic ease-in-out, accelerating then decelerating.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float quadInOut(float t) {
    return t <= .5 ? t * t * 2 : 1 - (--t) * t * 2;
  }

  /**
   * Applies cubic ease-in, accelerating slowly from zero with more force than quadratic.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float cubeIn(float t) {
    return t * t * t;
  }

  /**
   * Applies cubic ease-out, decelerating smoothly to zero with more force than quadratic.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float cubeOut(float t) {
    return 1 + (--t) * t * t;
  }

  /**
   * Applies cubic ease-in-out, accelerating then decelerating with more force than quadratic.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float cubeInOut(float t) {
    return t <= .5 ? t * t * t * 4 : 1 + (--t) * t * t * 4;
  }

  /**
   * Applies quartic ease-in, accelerating from zero with strong initial force.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float quartIn(float t) {
    return t * t * t * t;
  }

  /**
   * Applies quartic ease-out, decelerating to zero with strong braking force.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float quartOut(float t) {
    return 1 - (t -= 1) * t * t * t;
  }

  /**
   * Applies quartic ease-in-out, with a strong acceleration and deceleration curve.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float quartInOut(float t) {
    return t <= .5 ? t * t * t * t * 8 : (float) ((1 - (t = t * 2 - 2) * t * t * t) / 2 + .5);
  }

  /**
   * Applies quintic ease-in, accelerating from zero with very strong initial force.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float quintIn(float t) {
    return t * t * t * t * t;
  }

  /**
   * Applies quintic ease-out, decelerating to zero with very strong braking force.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float quintOut(float t) {
    return (t = t - 1) * t * t * t * t + 1;
  }

  /**
   * Applies quintic ease-in-out, with a very strong acceleration and deceleration curve.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float quintInOut(float t) {
    return ((t *= 2) < 1) ? (t * t * t * t * t) / 2 : ((t -= 2) * t * t * t * t + 2) / 2;
  }

  /**
   * Applies smooth-step ease-in using a cubic Hermite curve, with a gentle start.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float smoothStepIn(float t) {
    return 2 * smoothStepInOut(t / 2);
  }

  /**
   * Applies smooth-step ease-out using a cubic Hermite curve, with a gentle end.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float smoothStepOut(float t) {
    return 2 * smoothStepInOut((float) (t / 2 + 0.5)) - 1;
  }

  /**
   * Applies smooth-step ease-in-out using a cubic Hermite curve, with gentle start and end.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float smoothStepInOut(float t) {
    return t * t * (t * -2 + 3);
  }

  /**
   * Applies smoother-step ease-in using a quintic Hermite curve, with an even gentler start.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float smootherStepIn(float t) {
    return 2 * smootherStepInOut(t / 2);
  }

  /**
   * Applies smoother-step ease-out using a quintic Hermite curve, with an even gentler end.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float smootherStepOut(float t) {
    return 2 * smootherStepInOut((float) (t / 2 + 0.5)) - 1;
  }

  /**
   * Applies smoother-step ease-in-out using a quintic Hermite curve, with even gentler start
   * and end curves than {@link #smoothStepInOut}.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float smootherStepInOut(float t) {
    return t * t * t * (t * (t * 6 - 15) + 10);
  }

  /**
   * Applies sinusoidal ease-in, accelerating from zero along a sine curve.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float sineIn(float t) {
    return (float) (-Math.cos(PI2 * t) + 1);
  }

  /**
   * Applies sinusoidal ease-out, decelerating to zero along a sine curve.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float sineOut(float t) {
    return (float) Math.sin(PI2 * t);
  }

  /**
   * Applies sinusoidal ease-in-out, with both halves following a sine curve.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float sineInOut(float t) {
    return (float) (-Math.cos(Math.PI * t) / 2 + .5);
  }

  /**
   * Applies bounce ease-in, simulating a bouncing effect at the start of the motion.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float bounceIn(float t) {
    return 1 - bounceOut(1 - t);
  }

  /**
   * Applies bounce ease-out, simulating a bouncing effect at the end of the motion.
   *
   * <p>Uses a piecewise polynomial that models three diminishing bounces as the value
   * approaches its target.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float bounceOut(float t) {
    if (t < B1) {
      return (float) (7.5625 * t * t);
    }
    if (t < B2) {
      return (float) (7.5625 * (t - B3) * (t - B3) + .75);
    }
    if (t < B4) {
      return (float) (7.5625 * (t - B5) * (t - B5) + .9375);
    }
    return (float) (7.5625 * (t - B6) * (t - B6) + .984375);
  }

  /**
   * Applies bounce ease-in-out, simulating a bouncing effect at both ends of the motion.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float bounceInOut(float t) {
    return t < 0.5 ? (1 - bounceOut(1 - 2 * t)) / 2 : (1 + bounceOut(2 * t - 1)) / 2;
  }

  /**
   * Applies circular ease-in, accelerating from zero along the curve of a quarter circle.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float circIn(float t) {
    return (float) -(Math.sqrt(1 - t * t) - 1);
  }

  /**
   * Applies circular ease-out, decelerating to zero along the curve of a quarter circle.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float circOut(float t) {
    return (float) Math.sqrt(1 - (t - 1) * (t - 1));
  }

  /**
   * Applies circular ease-in-out, with both halves following the curve of a quarter circle.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float circInOut(float t) {
    return (float) (t <= .5
        ? (Math.sqrt(1 - t * t * 4) - 1) / -2
        : (Math.sqrt(1 - (t * 2 - 2) * (t * 2 - 2)) + 1) / 2);
  }

  /**
   * Applies exponential ease-in, accelerating rapidly from near-zero using a power of 2.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float expoIn(float t) {
    return (float) Math.pow(2, 10 * (t - 1));
  }

  /**
   * Applies exponential ease-out, decelerating rapidly to near-zero using a power of 2.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float expoOut(float t) {
    return (float) (-Math.pow(2, -10 * t) + 1);
  }

  /**
   * Applies exponential ease-in-out, with rapid acceleration and deceleration using a power of 2.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float expoInOut(float t) {
    return (float) (t < .5 ? Math.pow(2, 10 * (t * 2 - 1)) / 2 : (-Math.pow(2, -10 * (t * 2 - 1)) + 2) / 2);
  }

  /**
   * Applies back ease-in, pulling slightly behind the start before moving forward.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float backIn(float t) {
    return (float) (t * t * (2.70158 * t - 1.70158));
  }

  /**
   * Applies back ease-out, overshooting the end slightly before settling into place.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float backOut(float t) {
    return (float) (1 - (--t) * (t) * (-2.70158 * t - 1.70158));
  }

  /**
   * Applies back ease-in-out, pulling back at the start and overshooting at the end.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float backInOut(float t) {
    t *= 2;
    if (t < 1) {
      return (float) (t * t * (2.70158 * t - 1.70158) / 2);
    }
    t--;
    return (float) ((1 - (--t) * (t) * (-2.70158 * t - 1.70158)) / 2 + .5);
  }

  /**
   * Applies elastic ease-in, oscillating inward like a spring being stretched before release.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float elasticIn(float t) {
    return (float) -(ELASTIC_AMPLITUDE
        * Math.pow(2, 10 * (t -= 1))
        * Math.sin(
            (t - (ELASTIC_PERIOD / (2 * Math.PI) * Math.asin(1 / ELASTIC_AMPLITUDE)))
                * (2 * Math.PI)
                / ELASTIC_PERIOD));
  }

  /**
   * Applies elastic ease-out, oscillating outward like a released spring settling into place.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float elasticOut(float t) {
    return (float) (ELASTIC_AMPLITUDE
        * Math.pow(2, -10 * t)
        * Math.sin(
            (t - (ELASTIC_PERIOD / (2 * Math.PI) * Math.asin(1 / ELASTIC_AMPLITUDE)))
                * (2 * Math.PI)
                / ELASTIC_PERIOD)
        + 1);
  }

  /**
   * Applies elastic ease-in-out, oscillating like a spring at both the start and end.
   *
   * @param t The normalized progress, from 0 to 1.
   * @return The eased value.
   */
  public static float elasticInOut(float t) {
    if (t < 0.5) {
      return (float) (-0.5
          * (Math.pow(2, 10 * (t -= 0.5f))
              * Math.sin((t - (ELASTIC_PERIOD / 4)) * (2 * Math.PI) / ELASTIC_PERIOD)));
    }
    return (float) (Math.pow(2, -10 * (t -= 0.5f))
        * Math.sin((t - (ELASTIC_PERIOD / 4)) * (2 * Math.PI) / ELASTIC_PERIOD)
        * 0.5
        + 1);
  }
}
