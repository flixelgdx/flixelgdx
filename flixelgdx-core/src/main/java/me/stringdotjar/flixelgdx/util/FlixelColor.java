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
 *
 * <p>Shared presets such as {@link #WHITE} and {@link #RED} point at the same instances as libGDX
 * {@link Color} namesakes. Mutating a preset affects every place that uses that reference. For a
 * private copy, use {@code new FlixelColor(FlixelColor.RED)} or {@link #setColor(FlixelColor)} on your
 * own instance.
 *
 * <p>Use {@link #getGdxColor()} when you need component access, lerping, or batch tinting; use
 * {@link #getColor()} when you need a compact RGBA8888 value.
 */
public class FlixelColor {

  public static final FlixelColor WHITE = new FlixelColor(Color.WHITE);
  public static final FlixelColor BLACK = new FlixelColor(Color.BLACK);
  public static final FlixelColor RED = new FlixelColor(Color.RED);
  public static final FlixelColor GREEN = new FlixelColor(Color.GREEN);
  public static final FlixelColor BLUE = new FlixelColor(Color.BLUE);
  public static final FlixelColor YELLOW = new FlixelColor(Color.YELLOW);
  public static final FlixelColor CYAN = new FlixelColor(Color.CYAN);
  public static final FlixelColor MAGENTA = new FlixelColor(Color.MAGENTA);
  public static final FlixelColor GRAY = new FlixelColor(Color.GRAY);
  public static final FlixelColor CLEAR = new FlixelColor(Color.CLEAR);
  public static final FlixelColor ORANGE = new FlixelColor(Color.ORANGE);
  public static final FlixelColor PINK = new FlixelColor(Color.PINK);
  public static final FlixelColor PURPLE = new FlixelColor(Color.PURPLE);
  public static final FlixelColor BROWN = new FlixelColor(Color.BROWN);
  public static final FlixelColor OLIVE = new FlixelColor(Color.OLIVE);
  public static final FlixelColor MAROON = new FlixelColor(Color.MAROON);
  public static final FlixelColor NAVY = new FlixelColor(Color.NAVY);
  public static final FlixelColor TEAL = new FlixelColor(Color.TEAL);

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

  /**
   * @return Packed RGBA8888, same as libGDX {@link Color#rgba8888(Color)} on the backing color.
   */
  public int getColor() {
    return Color.rgba8888(color);
  }

  /**
   * Copies RGBA from {@code other} into this wrapper.
   *
   * @param other The libGDX color to copy. Must not be {@code null}.
   */
  public void setColor(@NotNull Color other) {
    color.set(other);
  }

  /**
   * Copies RGBA from {@code other} into this wrapper.
   *
   * @param other The Flixel color to copy. Must not be {@code null}.
   */
  public void setColor(@NotNull FlixelColor other) {
    color.set(other.color);
  }

  /**
   * @return The backing libGDX color (mutable). Must not be {@code null}.
   */
  @NotNull
  public Color getGdxColor() {
    return color;
  }
}
