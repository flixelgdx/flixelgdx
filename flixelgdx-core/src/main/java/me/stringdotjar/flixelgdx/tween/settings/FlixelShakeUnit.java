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
package me.stringdotjar.flixelgdx.tween.settings;

import me.stringdotjar.flixelgdx.functional.FlixelShakeable;
import me.stringdotjar.flixelgdx.tween.type.FlixelShakeTween;
import me.stringdotjar.flixelgdx.util.FlixelAxes;

/**
 * How {@link FlixelShakeTween#intensity} maps to maximum offset per axis.
 *
 * <p>{@link #FRACTION} scales horizontal range by {@link FlixelShakeable#getShakeWidth()} and vertical range by
 * {@link FlixelShakeable#getShakeHeight()} when those return values greater than zero (per axis enabled by
 * {@link FlixelAxes}). Typical values are small, for example {@code 0.05f}.
 *
 * <p>{@link #PIXELS} sets the half-range in pixels on each active axis (random offset in {@code [-intensity, +intensity]}).
 */
public enum FlixelShakeUnit {

  /**
   * Scale intensity by {@link FlixelShakeable#getShakeWidth()} (X) and
   * {@link FlixelShakeable#getShakeHeight()} (Y) when positive. This is the default value.
   *
   * <p>Use this if you're familiar with HaxeFlixel and want to use the same behavior.
   */
  FRACTION,

  /**
   * Treat intensity as pixel magnitude on each shaken axis.
   *
   * <p>Use this if you want to directly set the half-range in pixels on each active
   * axis (random offset in {@code [-intensity, +intensity]}) and you're used to how
   * Unity or Godot shakes objects.
   */
  PIXELS
}
