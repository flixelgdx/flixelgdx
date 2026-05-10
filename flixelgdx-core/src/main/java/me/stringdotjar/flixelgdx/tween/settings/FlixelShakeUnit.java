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
