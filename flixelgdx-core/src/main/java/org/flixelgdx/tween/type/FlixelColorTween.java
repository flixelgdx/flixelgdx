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

import org.flixelgdx.functional.FlixelColorable;
import org.flixelgdx.tween.FlixelTween;
import org.flixelgdx.tween.settings.FlixelTweenSettings;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Interpolates between two colors and optionally applies tint to a {@link FlixelColorable}.
 */
public class FlixelColorTween extends FlixelTween {

  protected final FlixelColor workFrom = new FlixelColor();
  protected final FlixelColor workTo = new FlixelColor();
  protected final FlixelColor workOut = new FlixelColor();

  @Nullable
  protected FlixelColor fromFlixel;
  @Nullable
  protected FlixelColor toFlixel;
  @Nullable
  protected FlixelColorable colorTarget;
  @Nullable
  protected Runnable onColor;
  protected boolean useRawColor;

  /** Creates a new color tween with the given tween settings. */
  public FlixelColorTween(@Nullable FlixelTweenSettings settings) {
    super(settings);
  }

  /**
   * Tween between two {@link FlixelColor} endpoints.
   *
   * @param target The tint target to tween.
   * @param from The starting color.
   * @param to The ending color.
   * @param onColor The callback to run when the tween is complete.
   * @return {@code this} for chaining.
   */
  public FlixelColorTween setColorEndpoints(@Nullable FlixelColorable target, @Nullable FlixelColor from,
      @Nullable FlixelColor to, @Nullable Runnable onColor) {
    this.useRawColor = false;
    this.colorTarget = target;
    this.fromFlixel = from;
    this.toFlixel = to;
    this.onColor = onColor;
    return this;
  }

  /**
   * Tween between two color values copied into internal buffers, so later changes to the
   * arguments do not affect the running tween.
   *
   * @param target The tint target to tween.
   * @param from The starting color.
   * @param to The ending color.
   * @param onColor The callback to run when the tween is complete.
   * @return {@code this} for chaining.
   */
  public FlixelColorTween setColorEndpointsRaw(@Nullable FlixelColorable target, @Nullable FlixelColor from,
      @Nullable FlixelColor to, @Nullable Runnable onColor) {
    this.useRawColor = true;
    this.colorTarget = target;
    this.fromFlixel = null;
    this.toFlixel = null;
    this.onColor = onColor;
    if (from != null) {
      workFrom.setColor(from);
    }
    if (to != null) {
      workTo.setColor(to);
    }
    return this;
  }

  @Override
  protected void updateTweenValues() {
    if (useRawColor) {
      workOut.setColor(workFrom);
      workOut.lerp(workTo, scale);
    } else {
      if (fromFlixel == null || toFlixel == null) {
        return;
      }
      workOut.setColor(fromFlixel);
      workOut.lerp(toFlixel, scale);
    }

    if (colorTarget != null) {
      colorTarget.setColor(workOut);
    }
    if (onColor != null) {
      onColor.run();
    }
  }

  @Override
  public boolean isTweenOf(Object object, String field) {
    if (colorTarget == null) {
      return false;
    }
    if (field == null || field.isEmpty()) {
      return Objects.equals(object, colorTarget);
    }
    return Objects.equals(object, colorTarget) && "color".equals(field);
  }

  @Override
  public void reset() {
    super.reset();
    colorTarget = null;
    fromFlixel = null;
    toFlixel = null;
    onColor = null;
    useRawColor = false;
  }
}
