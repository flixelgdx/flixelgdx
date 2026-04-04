/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

import org.jetbrains.annotations.NotNull;

/**
 * Mutable color wrapper that owns a single {@link Color} instance for stable tinting
 * and tween endpoints without per-frame allocations.
 */
public final class FlixelColor {

  @NotNull
  private final Color color;

  /**
   * Creates a new color with the default white color.
   */
  public FlixelColor() {
    this.color = new Color(Color.WHITE);
  }

  /**
   * Creates a new color with the given RGBA values. Values must be in the range 0-255.
   *
   * @param r The red component.
   * @param g The green component.
   * @param b The blue component.
   * @param a The alpha component.
   */
  public FlixelColor(int r, int g, int b, int a) {
    float nr = MathUtils.clamp(r, 0, 255) / 255f;
    float ng = MathUtils.clamp(g, 0, 255) / 255f;
    float nb = MathUtils.clamp(b, 0, 255) / 255f;
    float na = MathUtils.clamp(a, 0, 255) / 255f;
    this.color = new Color(nr, ng, nb, na);
  }

  /**
   * Creates a new color with the given RGBA values. Values must be in the range 0-1.
   *
   * @param r The red component.
   * @param g The green component.
   * @param b The blue component.
   * @param a The alpha component.
   */
  public FlixelColor(float r, float g, float b, float a) {
    this.color = new Color(r, g, b, a);
  }

  /**
   * Creates a new color from the given packed RGBA8888 value.
   *
   * @param rgba8888 The packed RGBA8888 value.
   */
  public FlixelColor(int rgba8888) {
    this.color = new Color(rgba8888);
  }

  /**
   * Creates a new color from the given {@link Color} value.
   *
   * @param source The {@link Color} value to copy.
   */
  public FlixelColor(@NotNull Color source) {
    this.color = new Color(source);
  }

  @NotNull
  public Color getGdxColor() {
    return color;
  }

  public float red() {
    return color.r;
  }

  public float green() {
    return color.g;
  }

  public float blue() {
    return color.b;
  }

  public float alpha() {
    return color.a;
  }

  /**
   * Sets the color to the given RGBA values. Values must be in the range 0-255.
   *
   * @param r The red component.
   * @param g The green component.
   * @param b The blue component.
   * @param a The alpha component.
   * @return {@code this} for chaining.
   */
  public FlixelColor set(int r, int g, int b, int a) {
    float nr = MathUtils.clamp(r, 0, 255) / 255f;
    float ng = MathUtils.clamp(g, 0, 255) / 255f;
    float nb = MathUtils.clamp(b, 0, 255) / 255f;
    float na = MathUtils.clamp(a, 0, 255) / 255f;
    color.set(nr, ng, nb, na);
    return this;
  }

  /**
   * Sets the color to the given RGBA values. Values must be in the range 0-1.
   *
   * @param r The red component.
   * @param g The green component.
   * @param b The blue component.
   * @param a The alpha component.
   * @return {@code this} for chaining.
   */
  public FlixelColor set(float r, float g, float b, float a) {
    color.set(r, g, b, a);
    return this;
  }

  /**
   * Sets the color to the given {@link Color} value.
   *
   * @param other The {@link Color} value to copy.
   * @return {@code this} for chaining.
   */
  public FlixelColor set(@NotNull Color other) {
    color.set(other);
    return this;
  }

  /**
   * Sets the color to the given {@link FlixelColor} value.
   *
   * @param other The {@link FlixelColor} value to copy.
   * @return {@code this} for chaining.
   */
  public FlixelColor set(@NotNull FlixelColor other) {
    color.set(other.color);
    return this;
  }

  public int pack() {
    return Color.rgba8888(color);
  }

  /**
   * Linearly interpolates between this color and {@code to} by {@code t}.
   *
   * @param to The color to interpolate to.
   * @param t The interpolation factor.
   * @return {@code this} for chaining.
   */
  public FlixelColor lerp(@NotNull FlixelColor to, float t) {
    color.lerp(to.color, MathUtils.clamp(t, 0f, 1f));
    return this;
  }

  /**
   * Mutates this color from HSV (h degrees, s/v 0–1), then sets alpha.
   *
   * @param h The hue.
   * @param s The saturation.
   * @param v The value.
   * @param a The alpha.
   * @return {@code this} for chaining.
   */
  public FlixelColor fromHsv(float h, float s, float v, float a) {
    color.fromHsv(h, s, v);
    color.a = a;
    return this;
  }
}
